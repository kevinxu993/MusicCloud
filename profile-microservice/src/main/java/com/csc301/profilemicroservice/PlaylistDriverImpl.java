package com.csc301.profilemicroservice;

import okhttp3.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import org.springframework.util.StringUtils;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			String listName = userName + "-favorites";
			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			queryStr = "MATCH (nPlaylist:playlist{plName:'" + listName + "'}),(nSong:song{songID:'" + songId + "'}) " +
					"MERGE (nPlaylist)-[rela:includes]->(nSong) RETURN rela";
			trans.run(queryStr);
			trans.success();
			// communicate with song microservice
			OkHttpClient client = new OkHttpClient();
			HttpUrl httpUrl = new HttpUrl.Builder()
					.scheme("http")
					.host("localhost:3001")
					.addPathSegment("likeSong")
					.addPathSegment(songId)
					.addQueryParameter("shouldDecrement", "false")
					.build();
			System.out.println(httpUrl.toString());
			RequestBody body = RequestBody.create("", JSON);
			Request request = new Request.Builder()
					.addHeader("accept", "application/json")
					.url(httpUrl) // <- Finally put httpUrl in here
					.put(body)
					.build();
			Response response = client.newCall(request).execute();
			System.out.println(response);
			dbQueryStatus.setMessage("User " + userName + "successfully liked the Song");
		} catch (Exception ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			String listName = userName + "-favorites";
			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			queryStr = "MATCH (:playlist{plName:'" + listName + "'})-[rela:includes]->" +
					"(:song{songID:'" + songId + "'}) DELETE rela";
			trans.run(queryStr);
			trans.success();
			dbQueryStatus.setMessage("User " + userName + "successfully liked the Song");
		} catch (Exception ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		return null;
	}
}
