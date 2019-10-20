package ru.mvideo.test.couchbase.steps;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Step;
import ru.mvideo.test.couchbase.manager.CouchBaseManager;

public class CouchBaseSteps {

	private Cluster cluster;
	private Bucket bucket;

	@Step
	public void openClusterConnect() {
		cluster = CouchBaseManager.getClusterConnection();
	}

	@Step
	@SuppressWarnings(value = "unchecked")
	public void executeN1qlQuery(String cbName, String n1qlQuery) {
		bucket = CouchBaseManager.getBucketConnection(cluster, cbName);
		CouchBaseManager.n1qlQuery( bucket, n1qlQuery);
		CouchBaseManager.closeBucketConnection(bucket);
	}

	@Step
	public void executeN1qlParamQuery(String cbName, String n1qlQuery, String varName) {
		bucket = CouchBaseManager.getBucketConnection(cluster, cbName);
		CouchBaseManager.n1qlParametrizedQuery(bucket, n1qlQuery, varName, Serenity.getCurrentSession().get(varName).toString());
		CouchBaseManager.actualQueryResult(bucket, n1qlQuery, varName, Serenity.getCurrentSession().get(varName).toString());
		CouchBaseManager.closeBucketConnection(bucket);
	}

	@Step
	public void closeCluster() {
		CouchBaseManager.closeClusterConnection(cluster);
	}
}