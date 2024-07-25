package com.example.watchlist;

import java.util.List;
import java.util.Map;
import com.google.firebase.Timestamp;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FirebaseApi {

    @GET("getTeams")
    Call<List<String>> getTeams();


    @DELETE("deleteTeam")
    Call<Void> deleteTeam(@Query("teamName") String teamName);

    @POST("addTeam")
    Call<Void> addTeam(@Body Team team);


    @PUT("updateMembers")
    Call<Void> updateMembers(@Query("teamName") String teamName, @Body Map<String, String> members);

    @DELETE("deleteMember")
    Call<Void> deleteMember(@Query("teamName") String teamName, @Query("memberName") String memberName);

    @GET("getMembers")
    Call<Map<String, String>> getMembers(@Query("teamName") String teamName);

    @PUT("changeTeamName")
    Call<Void> changeTeamName(@Query("oldTeamName") String oldTeamName, @Query("newTeamName") String newTeamName);

    @GET("getWatchLists")
    Call<List<WatchList>> getWatchLists(@Query("teamName") String teamName);

    @POST("createWatchList")
    Call<Void> createWatchList(@Body WatchList watchList);

    @DELETE("deleteWatchList")
    Call<Void> deleteWatchList(@Query("teamName") String teamName, @Query("listName") String listName);

    @GET("getWatchList")
    Call<Map<String, Object>> getWatchList(@Query("teamName") String teamName, @Query("listName") String listName);

    @POST("saveSchedule")
    Call<Void> saveSchedule(@Query("teamName") String teamName, @Query("listName") String listName, @Body Map<String, Object> scheduleData);

    @DELETE("deleteList")
    Call<Void> deleteList(@Query("teamName") String teamName, @Query("listName") String listName);

    @POST("addList")
    Call<Void> addList(@Query("teamName") String teamName, @Body ListData listData);


}


class Team {
    private String name;

    public Team(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}


class ListData {
    private Map<String, Object> listData;

    public ListData(Map<String, Object> listData) {
        this.listData = listData;
    }

    public Map<String, Object> getListData() {
        return listData;
    }

    public void setListData(Map<String, Object> listData) {
        this.listData = listData;
    }
}

 class WatchList {
    private String teamName;
    private String listName;
    private long timestamp;

    public WatchList(String teamName, String listName, long timestamp) {
        this.teamName = teamName;
        this.listName = listName;
        this.timestamp = timestamp;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}