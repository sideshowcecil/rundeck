package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.tests.functional.selenium.pages.ProjectCreatePage
import org.rundeck.tests.functional.selenium.pages.ProjectPage
import org.rundeck.tests.functional.selenium.pages.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class NodesSpec extends SeleniumBase {

    def "go to edit nodes"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def homePage = page HomePage
            homePage.createProjectButton()
        then:
            def projectCreatePage = page ProjectCreatePage
            projectCreatePage.createProject(toCamelCase specificationContext.currentFeature.name)
            def projectPage = page ProjectPage
            'Add a new Node Source' == projectPage.newNodeSourceButton.getText()
        cleanup:
            def sideBarPage = page SideBarPage
            sideBarPage.deleteProject()
            sideBarPage.waitForModal 1
    }

}
