package ru.mvideo.test.couchbase.scenario;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import net.thucydides.core.annotations.Steps;
import ru.mvideo.test.couchbase.steps.CouchBaseSteps;
import ru.mvideo.test.core.session.SessionHelper;

public class CouchBaseScenario {

	@Steps
	private CouchBaseSteps couchBaseSteps;

	@Before
	public void beforeCouchBaseScenario() {
		SessionHelper.saveSessionVariables(SessionHelper.getKeptSessionVariables());
		couchBaseSteps.openClusterConnect();
	}

	@Then("^выполнено изменение состояния couchbase bucket \"([^\"]*)\":$")
	public void changeCBQuery(String cbName, String n1qlQuery) {
		couchBaseSteps.executeN1qlQuery(cbName, n1qlQuery);
	}

	@Then("^в couchbase bucket \"([^\"]*)\" выполнен запрос \"([^\"]*)\" в котором переменная принимает значение \"([^\"]*)\"$")
	public void dbDataShouldBeCompleted(String cbName, String n1qlQuery, String varName) {
		couchBaseSteps.executeN1qlParamQuery(cbName, n1qlQuery, varName);
	}

	@After
	public void afterCouchBaseScenario() {
		couchBaseSteps.closeCluster();
	}
}