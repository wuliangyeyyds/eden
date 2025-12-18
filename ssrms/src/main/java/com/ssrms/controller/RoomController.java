package com.ssrms.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ssrms.common.Result;
import com.ssrms.entity.Room;
import com.ssrms.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/campuses")
    public Result campuses() {
        List<String> list = roomService.list(
                Wrappers.<Room>lambdaQuery().eq(Room::getStatus, "open")
        ).stream().map(Room::getCampus).distinct().sorted().collect(Collectors.toList());
        return Result.success(list, (long) list.size());
    }

    @GetMapping("/buildings")
    public Result buildings(@RequestParam String campus) {
        List<String> list = roomService.list(
                Wrappers.<Room>lambdaQuery()
                        .eq(Room::getStatus, "open")
                        .eq(Room::getCampus, campus)
        ).stream().map(Room::getBuilding).distinct().sorted().collect(Collectors.toList());
        return Result.success(list, (long) list.size());
    }

    @GetMapping("/rooms")
    public Result rooms(@RequestParam String campus, @RequestParam String building) {
        List<Room> list = roomService.list(
                Wrappers.<Room>lambdaQuery()
                        .eq(Room::getStatus, "open")
                        .eq(Room::getCampus, campus)
                        .eq(Room::getBuilding, building)
        ).stream().sorted(Comparator.comparing(Room::getRoomName)).collect(Collectors.toList());
        return Result.success(list, (long) list.size());
    }

    /** 前端三选一确定 roomId 的时候用 */
    @GetMapping("/findOne")
    public Result findOne(@RequestParam String campus,
                          @RequestParam String building,
                          @RequestParam String roomName) {
        Room one = roomService.getOne(
                Wrappers.<Room>lambdaQuery()
                        .eq(Room::getCampus, campus)
                        .eq(Room::getBuilding, building)
                        .eq(Room::getRoomName, roomName)
                        .last("limit 1")
        );
        return Result.success(one);
    }

    /** ✅ 给前端直接拉全量房间用 */
    @GetMapping({"/list", "/all"})
    public Result listAllOpenRooms() {
        List<Room> list = roomService.list(
                Wrappers.<Room>lambdaQuery()
                        .eq(Room::getStatus, "open")
                        .orderByAsc(Room::getCampus)
                        .orderByAsc(Room::getBuilding)
                        .orderByAsc(Room::getRoomName)
        );
        return Result.success(list, (long) list.size());
    }
}