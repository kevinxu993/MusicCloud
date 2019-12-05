package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(fullName) || StringUtils.isEmpty(password)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			String listName = userName + "-favorites";
			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();

			// determine userName if it exists
			queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName='" + userName +"' return nProfile.userName;";
			StatementResult sr = trans.run(queryStr);
			if (sr.hasNext()) {
				dbQueryStatus.setMessage("User " + userName + " already EXISTS!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			queryStr = "CREATE (nProfile:profile{userName:'" + userName + "',fullName:'" +fullName+
					"',password:'" + password + "'}) return nProfile;";
			trans.run(queryStr);


			queryStr = "CREATE (nPlaylist:playlist{plName:'" + listName + "'}) return nPlaylist;";
			trans.run(queryStr);

			queryStr = "MATCH (nProfile:profile{userName:'" + userName + "'}),(nPlaylist:playlist{plName:'"
					+ listName + "'}) CREATE (nProfile)-[r:created]->(nPlaylist) return r;";
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

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(frndUserName)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}

			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			// determine userName if it exists
			queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName='" + userName +"' return nProfile.userName;";
			StatementResult sr = trans.run(queryStr);
			if ( ! sr.hasNext()) {
				dbQueryStatus.setMessage("User " + userName + " DOES NOT EXIST!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			// determine frndUserName if it exists
			queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName='" + frndUserName +"' return nProfile.userName;";
			sr = trans.run(queryStr);
			if ( ! sr.hasNext()) {
				dbQueryStatus.setMessage("User " + frndUserName + " DOES NOT EXIST!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			queryStr = "MATCH (nProfile:profile{userName:'" + userName + "'}),(mProfile:profile{userName:'"
					+ frndUserName + "'}) CREATE (nProfile)-[rela:follows]->(mProfile) return rela;";
			trans.run(queryStr);
			trans.success();
			dbQueryStatus.setMessage("User " + userName + "successfully follows User" + frndUserName);
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
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		String queryStr;
		Session session = null;
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {

			if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(frndUserName)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}


			session = ProfileMicroserviceApplication.driver.session();
			Transaction trans = session.beginTransaction();
			queryStr = "MATCH (nProfile),(mProfile) WHERE nProfile.userName='" + userName +
					"' AND mProfile.userName='" + frndUserName +
					"' MATCH (nProfile)-[rela:follows]-(mProfile) DELETE rela";
			trans.run(queryStr);
			trans.success();
			dbQueryStatus.setMessage("User " + userName + "successfully unfollowed User" + frndUserName);
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
	public DbQueryStatus getAllSongFriendsLike(String userName) {

		return null;
	}
}
