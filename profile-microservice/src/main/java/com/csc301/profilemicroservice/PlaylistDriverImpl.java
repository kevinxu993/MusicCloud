package com.csc301.profilemicroservice;

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

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		return null;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		return null;
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

			// determine songId if it not exists
			queryStr = "MATCH (mSong:song) WHERE mSong.songId='" + songId +"' return mSong.songId;";
			StatementResult sr = trans.run(queryStr);
			if ( ! sr.hasNext()) {
				dbQueryStatus.setMessage("Song " + songId + " DOES NOT EXIST!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			// delete favourite
			queryStr = "MATCH (mSong:song) WHERE mSong.songId='"+ songId +"'  MATCH (nPlaylist)-[r:includes]->(mSong) DELETE r;";
			trans.run(queryStr);

			// delete node
			queryStr = "MATCH (mSong:song) WHERE mSong.songId='" + songId +"' DELETE mSong;";
			trans.run(queryStr);

			trans.success();
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
