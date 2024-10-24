/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.utils

import com.dtolabs.rundeck.core.utils.ResourceAcceptanceTimeoutException
import com.dtolabs.rundeck.core.utils.WaitUtils
import spock.lang.Specification

import java.time.Duration

class WaitUtilsSpec extends Specification {

    def testWaitForThatNeverAccepts() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return 0
        }

        when:
        WaitUtils.waitFor(retriever, { !!it}, Duration.ofMillis(500), Duration.ofMillis(110) )

        then:
        def e = thrown(ResourceAcceptanceTimeoutException)
        e.message == "Timeout reached (500ms) waiting for value: 0 to reach the desired state"
        closure_loop_counter == 5
    }

    def testWaitForThatAcceptsRightAway() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return 1
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it}, Duration.ofSeconds(1), Duration.ofMillis(100) )

        then:
        result == 1
        closure_loop_counter == 1
    }

    def testWaitForThatAcceptsEventually() {
        given:

        int closure_loop_counter = 0
        def vals = [null, 1]
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return vals[closure_loop_counter - 1]
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it}, Duration.ofSeconds(1), Duration.ofMillis(100) )

        then:
        result == 1
        closure_loop_counter == 2
    }
}
