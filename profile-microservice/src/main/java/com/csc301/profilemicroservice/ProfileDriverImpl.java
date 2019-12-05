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
		
		return null;
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
				dbQueryStatus.setMessage("User " + userName + " NOT EXIST!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			// determine frndUserName if it exists
			queryStr = "MATCH (nProfile:profile) WHERE nProfile.userName='" + frndUserName +"' return nProfile.userName;";
			sr = trans.run(queryStr);
			if ( ! sr.hasNext()) {
				dbQueryStatus.setMessage("User " + frndUserName + " NOT EXIST!");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			// determine follow if it exists
			queryStr = "MATCH (nProfile),(mProfile) WHERE nProfile.userName='" + userName + "' AND mProfile.userName='" + frndUserName + "' MATCH (nProfile)-[r:follows]-(mProfile) return r";
			sr = trans.run(queryStr);
			if ( sr.hasNext()) {
				dbQueryStatus.setMessage("User " + frndUserName + " already followed user " + frndUserName);
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
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
		
		return null;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
