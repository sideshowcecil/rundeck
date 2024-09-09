package org.rundeck.util.common

import groovy.transform.TypeChecked
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FirstParam
import groovy.transform.stc.FromString

import java.time.Duration

class WaitUtils  {

    /** Waits for the resource to be in the expected state by retrieving it and evaluating its acceptance as many times as needed.
     * @param retryableResourceRetriever executed multiple times to retrieve the current state of the R resource.
     * @param resourceAcceptanceEvaluator evaluates the state of the resource and and returns true if the resource is accepted.
     * @param timeout max duration to wait for the resource to reach the expected state.
     * @param checkPeriod time to wait between each check.
     * @return the resource that met the acceptance criteria.
     * @throws ResourceAcceptanceTimeoutException if the timeout is reached waiting for the resource to reach the expected state.
     */
    @TypeChecked
    static <R> R waitFor(
            Closure<R> retryableResourceRetriever,
            @ClosureParams(value = FromString, options = ["R"]) Closure<Boolean> resourceAcceptanceEvaluator = { R r -> !!r },
            Duration timeout = WaitingTime.MODERATE,
            Duration checkPeriod = WaitingTime.LOW) {
        R r = retryableResourceRetriever()
        Boolean acceptanceResult = resourceAcceptanceEvaluator(r)
        long initTime = System.currentTimeMillis()
        while (!acceptanceResult) {
            Thread.sleep(Math.min(checkPeriod.toMillis(), timeout.toMillis()))
            if ((System.currentTimeMillis() - initTime) >= timeout.toMillis()) {
                throw new ResourceAcceptanceTimeoutException("Timeout reached (${timeout.toSeconds()} seconds) waiting for ${r} to reach the desired state")
            }
            r = retryableResourceRetriever()
            acceptanceResult = resourceAcceptanceEvaluator(r)
        }
        return r
    }

    /** Waits for the resource to be in the expected state by retrieving it by its identifier and evaluating its acceptance as many times as needed.
     * @param resourceId resource identifier that is passable into the resourceRetriever as an input argument.
     * @param resourceRetriever closure that takes the RID resourceId and returns the current state of the R resource.
     * @param resourceAcceptanceEvaluator closure that evaluates the state of the resource by taking a R resource and returning true of the resource is accepted.
     * @param acceptanceFailureOutputProducer closure that produces a string output when the resource acceptance evaluator returns false to be included in the exception message.
     * @param timeout max duration to wait for the resource to reach the expected state.
     * @param checkPeriod time to wait between each check.
     * @return the resource that met the acceptance criteria.
     * @throws ResourceAcceptanceTimeoutException if the timeout is reached waiting for the resource to reach the expected state.
     */
    @TypeChecked
    static <RID, R> R waitForResource(RID resourceId,
                                      @ClosureParams(FirstParam.FirstGenericType.class) Closure<R> resourceRetriever,
                                      @ClosureParams(value = FromString, options = ["R"]) Closure<Boolean> resourceAcceptanceEvaluator = { R r -> true },
                                      @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> acceptanceFailureOutputProducer = { RID id -> "" },
                                      Duration timeout = WaitingTime.MODERATE,
                                      Duration checkPeriod = WaitingTime.LOW) {

        try {
            return waitFor({ resourceRetriever(resourceId) },
                    resourceAcceptanceEvaluator,
                    timeout,
                    checkPeriod)
        } catch (ResourceAcceptanceTimeoutException e) {
            // Enriches the exception message with the acceptanceFailureOutputProducer output and wrapping the original exception
            throw new ResourceAcceptanceTimeoutException("Timeout reached waiting for $resourceId ${acceptanceFailureOutputProducer(resourceId)} ", e)
        }
    }

    /** Waits for all resources to be in the expected state by retrieving them using identifiers and evaluating the acceptance as many times as needed.
     *  No guarantees are made about the order and timing of the retrieval.
     * @param resourceIdentifiers collection of resource identifiers. Each identifier should be passable into the resourceRetriever as an input argument.
     * @param resourceRetriever closure that takes the RID resourceId and returns the current state of the R resource.
     * @param resourceAcceptanceEvaluator closure that evaluates the state of the resource by taking a R resource and returning true of the resource is accepted.
     * @param timeout max duration to wait for the resource to reach the expected state.
     * @param checkPeriod time to wait between each check.
     * @param acceptanceFailureOutputProducer closure that produces a string output when the resource acceptance evaluator returns false to be included in the exception message.
     * @return a map of resource identifiers and their corresponding resources once the acceptance criteria is met by all.
     * @throws ResourceAcceptanceTimeoutException if the timeout is reached waiting for at least one resource to reach the expected state.
     *      If multiple resources fail to reach the expected state, the exception will be thrown for one them in non-deterministic order.
     */
    @TypeChecked
    static <RID, R> Map<RID, R> waitForAllResources(Collection<RID> resourceIdentifiers,
                                                    @ClosureParams(FirstParam.FirstGenericType.class) Closure<R> resourceRetriever,
                                                    @ClosureParams(value = FromString, options = ["R"]) Closure<Boolean> resourceAcceptanceEvaluator = { R r -> true },
                                                    Duration timeout = WaitingTime.MODERATE,
                                                    Duration checkPeriod = WaitingTime.LOW,
                                                    @ClosureParams(FirstParam.FirstGenericType.class) Closure<String> acceptanceFailureOutputProducer = { RID id -> "" }) {

        Map<RID, R> result = resourceIdentifiers.stream().reduce(
                new HashMap<>(),
                (Map<RID, R> acc, id) -> {
                    R r = waitForResource(id,
                            resourceRetriever,
                            resourceAcceptanceEvaluator,
                            acceptanceFailureOutputProducer,
                            timeout,
                            checkPeriod)
                    acc.put(id, r)
                    return acc;
                },
                (Map<RID, R> map1, Map<RID, R> map2) -> {
                    map1.putAll(map2);
                    return map1;
                }
        )
        return result
    }


    static <R> Builder<R> buildFor(Closure<R> retryableResourceRetriever) {
        return new Builder<>(retryableResourceRetriever)
    }

    static class Builder<R> {

        private Closure<R> retryableResourceRetriever = null
        private Closure<Boolean> resourceAcceptanceEvaluator = null
        private Duration timeout = null
        private Duration checkPeriod = null

        private Builder(Closure<R> retryableResourceRetriever) {
            this.retryableResourceRetriever = retryableResourceRetriever
        }


        public Builder<R> withResourceRetriever(Closure<R> retryableResourceRetriever) {
            this.retryableResourceRetriever = retryableResourceRetriever
            return this
        }

        public Builder<R> withResourceAcceptanceEvaluator(Closure<Boolean> resourceAcceptanceEvaluator) {
            this.resourceAcceptanceEvaluator = resourceAcceptanceEvaluator
            return this
        }

        public Builder<R> withTimeout(Duration timeout) {
            this.timeout = timeout
            return this
        }

        public Builder<R> withCheckPeriod(Duration checkPeriod) {
            this.checkPeriod = checkPeriod
            return this
        }

        public R doWait() {
            return waitFor(retryableResourceRetriever, resourceAcceptanceEvaluator, timeout, checkPeriod)
        }

    }

}
