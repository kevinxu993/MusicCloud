package com.csc301.songmicroservice;

import okhttp3.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(songToAdd.getSongName())
				|| StringUtils.isEmpty(songToAdd.getSongArtistFullName())
				|| StringUtils.isEmpty(songToAdd.getSongAlbum())) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			Object obj = db.insert(songToAdd);
			String songId = songToAdd.getId();
			String songName = songToAdd.getSongName();
			dbQueryStatus.setData(obj);
			// communicate with profile microservice
			OkHttpClient client = new OkHttpClient();
			RequestBody body = RequestBody.create("", JSON);
			Request request = new Request.Builder()
					.addHeader("accept", "application/json")
					.url("http://localhost:3002/addSong/?songId=" + songId + "&songName=" + songName)
					.post(body)
					.build();
			Response response = client.newCall(request).execute();
			//System.out.println(response);
		} catch (RuntimeException | IOException ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("Found song from DB", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			Object obj = db.findById(songId, Song.class);
			if (obj == null) {
				dbQueryStatus.setMessage("NOT_FOUND");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}
			dbQueryStatus.setData(obj);
		} catch (RuntimeException ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			Song obj = db.findById(songId, Song.class);
			if (obj == null) {
				dbQueryStatus.setMessage("NOT_FOUND");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}
			dbQueryStatus.setData(obj.getSongName());
		} catch (RuntimeException ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			Song obj = db.findById(songId, Song.class);

			if (obj == null) {
				dbQueryStatus.setMessage("NOT_FOUND");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			db.remove(obj);
//			dbQueryStatus.setData(obj);
			// communicate with profile microservice
			OkHttpClient client = new OkHttpClient();
			RequestBody body = RequestBody.create("", JSON);
			Request request = new Request.Builder()
					.addHeader("accept", "application/json")
					.url("http://localhost:3002/deleteAllSongsFromDb/" + songId)
					.put(body)
					.build();
			Response response = client.newCall(request).execute();
			//System.out.println(response);
		} catch (RuntimeException | IOException ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		}
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		DbQueryStatus dbQueryStatus = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		try {
			if (StringUtils.isEmpty(songId)) {
				dbQueryStatus.setMessage("ERROR_GENERIC");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				return dbQueryStatus;
			}
			Song obj = db.findById(songId, Song.class);

			if (obj == null) {
				dbQueryStatus.setMessage("NOT_FOUND");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return dbQueryStatus;
			}

			long amount = obj.getSongAmountFavourites();
			if (shouldDecrement) {
				if (amount >= 1) {
					obj.setSongAmountFavourites(amount - 1);
				} else {
					dbQueryStatus.setMessage("No negative favourities");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
					return dbQueryStatus;
				}
			} else {
				obj.setSongAmountFavourites(amount + 1);
			}
			obj = db.save(obj);
//			dbQueryStatus.setData(obj);
		} catch (RuntimeException ex) {
			dbQueryStatus.setMessage("ERROR_GENERIC");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			ex.printStackTrace();
		}
		return dbQueryStatus;
	}
}