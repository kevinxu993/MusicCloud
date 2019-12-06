package com.csc301.profilemicroservice;

import okhttp3.*;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
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
	public DbQueryStatus addSong(String songId, String songName) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			queryStr = "MERGE (:song{songId: '" + songId + "', songName: '" + songName + "'})";
			trans.run(queryStr);
			trans.success();
			dbQueryStatus.setMessage("Song with ID" + songId + "successfully created in neo4j database");
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
            queryStr = "MATCH (nPlaylist:playlist{plName:'" + listName + "'})-[:includes]->" +
                    "(nSong:song{songId:'" + songId + "'}) RETURN nPlaylist";
            StatementResult sr = trans.run(queryStr);
            if (!sr.hasNext()){
                queryStr = "MATCH (nPlaylist:playlist{plName:'" + listName + "'}),(nSong:song{songId:'" + songId + "'" +
                        "}) MERGE (nPlaylist)-[rela:includes]->(nSong) RETURN rela";
                trans.run(queryStr);
                trans.success();
                // communicate with song microservice
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create("", JSON);
                Request request = new Request.Builder()
                        .addHeader("accept", "application/json")
                        .url("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=false")
                        .put(body)
                        .build();
                Response response = client.newCall(request).execute();
                //System.out.println(response);
                dbQueryStatus.setMessage("User " + userName + "successfully liked the Song");
            }
            dbQueryStatus.setMessage("User " + userName + "already liked the Song");
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
            queryStr = "MATCH (nPlaylist:playlist{plName:'" + listName + "'})-[:includes]->" +
                    "(nSong:song{songId:'" + songId + "'}) RETURN nPlaylist";
            StatementResult sr = trans.run(queryStr);
            if (sr.hasNext()){
                queryStr = "MATCH (:playlist{plName:'" + listName + "'})-[rela:includes]->" +
                        "(:song{songId:'" + songId + "'}) DELETE rela";
                trans.run(queryStr);
                trans.success();
                // communicate with song microservice
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create("", JSON);
                Request request = new Request.Builder()
                        .addHeader("accept", "application/json")
                        .url("http://localhost:3001/updateSongFavouritesCount/" + songId + "?shouldDecrement=true")
                        .put(body)
                        .build();
                Response response = client.newCall(request).execute();
                //System.out.println(response);
                dbQueryStatus.setMessage("User " + userName + "successfully unliked the Song");
            }
            dbQueryStatus.setMessage("User " + userName + "didn't like the Song");
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
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			queryStr = "match (:playlist)-[rela:includes]->(nSong:song{songId:'"+ songId + "'}) delete rela, nSong";
			trans.run(queryStr);
			queryStr = "match (nSong:song{songId:'"+ songId + "'}) delete nSong";
			trans.run(queryStr);
			trans.success();
			dbQueryStatus.setMessage("successfully deletes the Song");
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
}
