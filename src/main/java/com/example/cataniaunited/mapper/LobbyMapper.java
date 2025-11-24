package com.example.cataniaunited.mapper;

import com.example.cataniaunited.dto.LobbyInfo;
import com.example.cataniaunited.lobby.Lobby;
import com.example.cataniaunited.player.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Set;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LobbyMapper {

    LobbyMapper INSTANCE = Mappers.getMapper(LobbyMapper.class);

    @Mapping(target = "id", source = "lobby.lobbyId")
    @Mapping(target = "hostPlayer", source = "hostPlayer", qualifiedByName = "mapHostPlayer")
    @Mapping(target = "playerCount", source = "lobby", qualifiedByName = "mapPlayerCount")
    LobbyInfo toDto(Lobby lobby, Player hostPlayer);

    @Named("mapPlayerCount")
    default int mapPlayerCount(Lobby lobby) {
        Set<String> players = lobby.getPlayers();
        return players.size();
    }

    @Named("mapHostPlayer")
    default String mapHostPlayer(Player hostPlayer) {
        if(hostPlayer == null){
            return null;
        }
        return hostPlayer.getUsername();
    }
}
