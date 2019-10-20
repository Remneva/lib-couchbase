package ru.mvideo.test.couchbase.manager;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.endpoint.kv.AuthenticationException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.auth.ClassicAuthenticator;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.core.Serenity;
import ru.mvideo.test.core.exception.IntegrationTestException;
import ru.mvideo.test.core.session.SessionVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.valueOf;

/**
 * Методы для работы с couchbase
 *
 * @author Remneva Maria {@literal <mariya.remneva@mvideo.ru>}
 */
@Slf4j
public class CouchBaseManager {

	private CouchBaseManager() {
	}

	private static final String CONNECTION_REFUSED_CB_CLUSTER = "Connection refused for Couchbase Cluster: ";
	private static final String CONNECTION_REFUSED_CB_BUCKET = "Connection refused for Couchbase Bucket: ";
	private static final String CLUSTER_CONNECTION_DOES_NOT_EXIST = "Cluster Connection does not exist";
	private static final String BUCKET_CONNECTION_DOES_NOT_EXIST = "Bucket Connection does not exist";
	private static final String QUERY_IS_NOT_CORRECT = "N1ql Query is not correct";
	private static final String ACTUAL_BODY_ERROR = "Can't put SessionVar ACTUAL_BODY";

	/**
	 * Установка соединения с кластером
	 */
	public static Cluster getClusterConnection() {
		try {
			Cluster cluster = CouchbaseCluster.create(Serenity.getCurrentSession().get(SessionVar.CB_HOST.getVarName()).toString());
			ClassicAuthenticator authenticator = new ClassicAuthenticator();
			authenticator.cluster((Serenity.getCurrentSession().get(SessionVar.CB_USERNAME.getVarName()).toString()),
					(Serenity.getCurrentSession().get(SessionVar.CB_PASSWORD.getVarName()).toString()));
			cluster.authenticate(authenticator);
			return cluster;
		} catch (Exception e) {
			throw new AuthenticationException(CONNECTION_REFUSED_CB_CLUSTER + e);
		}
	}

	/**
	 * Установка соединения с бакетом
	 */
	public static Bucket getBucketConnection(Cluster cluster, String cbName) {
		try {
			return cluster.openBucket(valueOf(cbName), (Serenity.getCurrentSession().get(SessionVar.CB_PASSWORD.getVarName()).toString()));
		} catch (Exception e) {
			throw new AuthenticationException(CONNECTION_REFUSED_CB_BUCKET + e);
		}
	}

	/**
	 * Simple N1ql запрос
	 */
	public static List<JsonObject> n1qlQuery(Bucket bucket, String n1qlQuery) {
		try {
			List<JsonObject> jsonObjects = new ArrayList<>();
			N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));
			if (result.status().equals("success")) {
				for (N1qlQueryRow row : result.allRows()) {
					jsonObjects.add(row.value());
				}
				return jsonObjects;
			} else {
				CouchBaseManager.closeBucketConnection(bucket);
				throw new IntegrationTestException(QUERY_IS_NOT_CORRECT);
			}
		} catch (IntegrationTestException e) {
			log.info(e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * Присваиваем значение сессионной переменной ACTUAL_BODY из результата запроса в couchbase
	 */
	public static void actualQueryResult(Bucket bucket, String n1qlQuery, String parameter, String value) {
		try {
			JsonObject executeResult = n1qlParametrizedQuery(bucket, n1qlQuery, parameter, value);
			if (executeResult != null) {
				Serenity.getCurrentSession().put(SessionVar.ACTUAL_BODY.getVarName(), executeResult.toString());
			}
		} catch (IntegrationTestException e) {
			throw new IntegrationTestException(ACTUAL_BODY_ERROR);
		}
	}

	/**
	 * Parameterized N1ql запрос
	 */
	public static JsonObject n1qlParametrizedQuery(Bucket bucket, String n1qlQuery, String varName, String value) {
		try {
			JsonObject placeholderValues = JsonObject.create().put(varName, value);
			N1qlQueryResult result = bucket.query(
					N1qlQuery.parameterized(n1qlQuery, placeholderValues));
			if (result.status().equals("success") && result.allRows().size() == 1) {
				return result.allRows().get(0).value();
			} else {
				CouchBaseManager.closeBucketConnection(bucket);
				throw new IntegrationTestException(QUERY_IS_NOT_CORRECT);
			}
		} catch (IntegrationTestException e) {
			log.info(e.getMessage());
		}
		return null;
	}

	/**
	 * Закрытие соединения с кластером и бакетом (сейчас не используется)
	 */
	public static void closeCouchBaseConnection(Cluster cluster, Bucket bucket) {
		try {
			if (bucket != null) {
				bucket.close();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new IllegalArgumentException(BUCKET_CONNECTION_DOES_NOT_EXIST);
		} finally {
			try {
				if (cluster != null) {
					cluster.disconnect();
				}
			} catch (RuntimeException e) {
				log.info(e.getMessage());
			}
		}
	}

	/**
	 * Закрытие соединения с кластером
	 */
	public static void closeClusterConnection(Cluster cluster) {
		try {
			if (cluster != null) {
				cluster.disconnect();
			}
		} catch (RuntimeException e) {
			log.info(e.getMessage());
			throw new CouchbaseException(CLUSTER_CONNECTION_DOES_NOT_EXIST);
		}
	}

	/**
	 * Закрытие соединения с бакетом
	 */
	public static void closeBucketConnection(Bucket bucket) {
		try {
			if (bucket != null) {
				bucket.close();
			}
		} catch (Exception e) {
			log.info(e.getMessage());
			throw new CouchbaseException(BUCKET_CONNECTION_DOES_NOT_EXIST);
		}
	}
}