import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectCreatePage} from 'pages/projectCreate.page'
import {LoginPage} from 'pages/login.page'
import {JobCreatePage} from 'pages/jobCreate.page'
import {JobShowPage} from "pages/jobShow.page"
import {until, By, Key, WebElement} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'
import {sleep} from '@rundeck/testdeck/async/util'
import {JobsListPage} from '../../../pages/jobsList.page'

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let jobCreatePage: JobCreatePage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    jobCreatePage = new JobCreatePage(ctx, 'SeleniumBasic')
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

const expectRadioValue=async function(elem:WebElement,val:boolean){
    let valuevalueShowExcluded = await elem.isSelected()
    expect(valuevalueShowExcluded).toBe(val)
}
const expectInputValue=async function(elem:WebElement,val:string){
    let valueText = await elem.getAttribute('value')
    expect(valueText).toBe(val)
}
const echologs = async () => {
    const logs = await ctx.driver.manage().logs().get('browser')
    // tslint:disable-next-line:prefer-for-of
    for (let i = 0 ; i < logs.length ; i++ ) {
        const value = logs[i]
        console.log(`${value.level} [${value.type}]: ${value.message} `)
    }
}
const createBasicJob = async (name: string) => {

    await jobCreatePage.get()
    await ctx.driver.wait(until.urlContains('/job/create'), 25000)
    const jobName = await jobCreatePage.jobNameInput()
    await jobName.sendKeys(name)

    // add workflow step
    const wfTab = await jobCreatePage.tabWorkflow()
    await wfTab.click()
    const addWfStepCommand = await jobCreatePage.addNewWfStepCommand()

    // click add Command step, and wait until input fields are loaded
    await addWfStepCommand.click()
    await jobCreatePage.waitWfStepCommandRemoteText()


    const wfStepCommandRemoteText = await jobCreatePage.wfStepCommandRemoteText()
    await wfStepCommandRemoteText.sendKeys('echo selenium test')

    const wfStep0SaveButton = await jobCreatePage.wfStep0SaveButton()

    // click step Save button and wait for the step content to display
    const wfstepEditDiv = await ctx.driver.findElement(By.css('#wfli_0 div.wfitemEditForm'))
    await wfStep0SaveButton.click()
    await jobCreatePage.waitWfstep0vis()

    // wait until edit form section for step is removed from dom
    await ctx.driver.wait(until.stalenessOf(wfstepEditDiv), 15000)
}

describe('job', () => {
    it('create job with dispatch to nodes', async () => {
        await createBasicJob('jobs with nodes')

        let tabNodes=await jobCreatePage.tabNodes()
        await tabNodes.click()

        let enableDispatchNodes= await jobCreatePage.dispatchNodes()
        await enableDispatchNodes.click()
        await sleep(500)

        let nodeFilter= await jobCreatePage.nodeFilter()
        expect(nodeFilter).toBeDefined()

        let nodeFilterMenuLink= await jobCreatePage.nodeFilterMenuLink()
        await nodeFilterMenuLink.click()

        let nodeFilterSelectAllLink= await jobCreatePage.nodeFilterSelectAllLink()
        await nodeFilterSelectAllLink.click()

        let matchedNodesText= await jobCreatePage.matchedNodesText()
        expect(matchedNodesText).toEqual("1 Node Matched")

        let showExcludedYes= await jobCreatePage.showExcludedNodesRadioYes()
        await showExcludedYes.click()

        let editableFilterYes= await jobCreatePage.editableFilterYes()
        await editableFilterYes.click()

        let schedJobnodeThreadcount= await jobCreatePage.schedJobnodeThreadcount()
        await schedJobnodeThreadcount.clear()
        await schedJobnodeThreadcount.sendKeys('3')

        let schedJobnodeRankAttribute= await jobCreatePage.schedJobnodeRankAttribute()
        await schedJobnodeRankAttribute.clear()
        await schedJobnodeRankAttribute.sendKeys('arank')

        let nodeRankOrderDescending= await jobCreatePage.nodeRankOrderDescending()
        await nodeRankOrderDescending.click()

        let nodeKeepgoingTrue= await jobCreatePage.nodeKeepgoingTrue()
        await nodeKeepgoingTrue.click()

        let successOnEmptyNodeFilterTrue= await jobCreatePage.successOnEmptyNodeFilterTrue()
        await successOnEmptyNodeFilterTrue.click()

        let nodesSelectedByDefaultFalse= await jobCreatePage.nodesSelectedByDefaultFalse()
        await nodesSelectedByDefaultFalse.click()

        //save the job
        let save = await jobCreatePage.saveButton()
        await save.click()

        let jobShowPage = new JobShowPage(ctx,'SeleniumBasic','')

        //verfiy job description
        await jobShowPage.waitJobDefinition()
        let jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNodefilters()
        let nodeFilterText = await jobShowPage.jobDefinitionNodeFilterMatchedText()
        expect(nodeFilterText).toEqual("Include nodes matching: name: .*")

        let threadcountText = await jobShowPage.jobDefinitionNodeThreadcountText()
        expect(threadcountText).toEqual("Execute on up to 3 Nodes at a time.")

        let keepgoingText = await jobShowPage.jobDefinitionNodeKeepgoingText()
        expect(keepgoingText).toEqual("If a node fails: Continue running on any remaining nodes before failing the step.")

        let orderingText = await jobShowPage.jobDefinitionNodeRankOrderAscendingText()
        expect(orderingText).toEqual("Sort nodes by arank in descending order.")

        let selectedByDefaultText = await jobShowPage.jobDefinitionNodeSelectedByDefaultText()
        expect(selectedByDefaultText).toEqual("Node selection: The user has to explicitly select target nodes")


        //close modal
        await jobShowPage.closeJobDefinitionModal()
        await sleep(500)

        let showUrl = await ctx.driver.getCurrentUrl()
        //change to edit page
        let editUrl = showUrl.replace('/job/show','/job/edit')

        //edit job and verify values are set in form

        await ctx.driver.navigate().to(editUrl)

        let jobEditPage = new JobCreatePage(ctx,'SeleniumBasic')


        let edittabNodes=await jobEditPage.tabNodes()
        await edittabNodes.click()

        let valueEnableDispatchNodes= await jobEditPage.dispatchNodes()
        await expectRadioValue(valueEnableDispatchNodes,true)

        let valueNodeFilter = await jobEditPage.nodeFilter()
        await expectInputValue(valueNodeFilter,'.*')

        let valueShowExcludedYes= await jobEditPage.showExcludedNodesRadioYes()
        await expectRadioValue(valueShowExcludedYes,true)

        let valueEditableFilterYes= await jobEditPage.editableFilterYes()
        await expectRadioValue(valueEditableFilterYes, true)

        let valueSchedJobnodeThreadcount= await jobEditPage.schedJobnodeThreadcount()
        await expectInputValue(valueSchedJobnodeThreadcount,'3')

        let valueSchedJobnodeRankAttribute= await jobEditPage.schedJobnodeRankAttribute()
        await expectInputValue(valueSchedJobnodeRankAttribute,'arank')

        let valueNodeRankOrderDescending= await jobEditPage.nodeRankOrderDescending()
        await expectRadioValue(valueNodeRankOrderDescending, true)

        let valueNodeKeepgoingTrue= await jobEditPage.nodeKeepgoingTrue()
        await expectRadioValue(valueNodeKeepgoingTrue, true)

        let valueSuccessOnEmptyNodeFilterTrue= await jobEditPage.successOnEmptyNodeFilterTrue()
        await expectRadioValue(valueSuccessOnEmptyNodeFilterTrue, true)

        let valuenodesSelectedByDefaultFalse= await jobEditPage.nodesSelectedByDefaultFalse()
        await expectRadioValue(valuenodesSelectedByDefaultFalse, true)

        const listPage = new JobsListPage(ctx, 'SeleniumBasic')
        await listPage.get()

    })

    it('rename job with orchestrator', async () => {
        await createBasicJob('job with node orchestrator')

        const tabNodes = await jobCreatePage.tabNodes()
        await tabNodes.click()

        const enableDispatchNodes = await jobCreatePage.dispatchNodes()
        await enableDispatchNodes.click()
        await sleep(500)

        const nodeFilter = await jobCreatePage.nodeFilter()
        expect(nodeFilter).toBeDefined()

        const nodeFilterMenuLink = await jobCreatePage.nodeFilterMenuLink()
        await nodeFilterMenuLink.click()

        const nodeFilterSelectAllLink = await jobCreatePage.nodeFilterSelectAllLink()
        await nodeFilterSelectAllLink.click()

        const matchedNodesText = await jobCreatePage.matchedNodesText()
        expect(matchedNodesText).toEqual('1 Node Matched')

        // choose orchestrator rankTiered

        const dropdown = await jobCreatePage.orchestratorDropdownButton()
        await dropdown.click()

        // select rankTiered
        const choice = await jobCreatePage.orchestratorChoice('rankTiered')
        await choice.click()

        // save the job
        const save = await jobCreatePage.saveButton()
        await save.click()

        const jobShowPage = new JobShowPage(ctx, 'SeleniumBasic', '')

        // verfiy job description
        await jobShowPage.waitJobDefinition()
        const jobDefinitionModal = await jobShowPage.jobDefinition()
        await jobDefinitionModal.click()

        await jobShowPage.waitDefinitionNodefilters()
        const orchName = await jobShowPage.jobDefinitionOrchestratorText()
        const orchText = await orchName.getText()
        expect(orchText).toEqual('Rank Tiered')

        // close modal
        await jobShowPage.closeJobDefinitionModal()
        await sleep(500)

        const showUrl = await ctx.driver.getCurrentUrl()

        // change to edit page
        const editUrl = showUrl.replace('/job/show', '/job/edit')

        // edit job and verify values are set in form

        await ctx.driver.navigate().to(editUrl)

        const jobEditPage = new JobCreatePage(ctx, 'SeleniumBasic')

        // rename job
        const nameInput = await jobEditPage.jobNameInput()
        await nameInput.clear()
        await nameInput.sendKeys('renamed job with node orchestrator')

        const save2 = await jobCreatePage.editSaveButton()
        await save2.click()

        await ctx.driver.wait(until.urlContains('/job/show'), 95000)
        const listPage = new JobsListPage(ctx, 'SeleniumBasic')
        await listPage.get()

    })
})