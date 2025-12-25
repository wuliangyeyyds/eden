package com.ssrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.CreateRoomDTO;
import com.ssrms.controller.vo.AdminRoomRowVO;
import com.ssrms.entity.Room;
import com.ssrms.entity.Seat;
import com.ssrms.mapper.ReservationMapper;
import com.ssrms.service.RoomService;
import com.ssrms.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/room/admin")
public class RoomAdminController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private ReservationMapper reservationMapper;

    @GetMapping("/campuses")
    public Result campuses(@RequestParam(value = "onlyOpen", required = false) Integer onlyOpen) {
        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.select("DISTINCT campus");
        if (onlyOpen != null && onlyOpen == 1) {
            qw.eq("status", "open");
        }
        qw.orderByAsc("campus");
        List<Room> rooms = roomService.list(qw);
        List<String> campuses = rooms.stream()
                .map(Room::getCampus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(campuses, (long) campuses.size());
    }

    @GetMapping("/buildings")
    public Result buildings(@RequestParam(value = "campus", required = false) String campus,
                            @RequestParam(value = "onlyOpen", required = false) Integer onlyOpen) {
        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.select("DISTINCT building");
        if (StringUtils.isNotBlank(campus)) {
            qw.eq("campus", campus);
        }
        if (onlyOpen != null && onlyOpen == 1) {
            qw.eq("status", "open");
        }
        qw.orderByAsc("building");
        List<Room> rooms = roomService.list(qw);
        List<String> buildings = rooms.stream()
                .map(Room::getBuilding)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return Result.success(buildings, (long) buildings.size());
    }

    @GetMapping("/list")
    public Result list(@RequestParam(value = "campus", required = false) String campus,
                       @RequestParam(value = "building", required = false) String building,
                       @RequestParam(value = "onlyOpen", required = false) Integer onlyOpen,
                       @RequestParam(value = "keyword", required = false) String keyword) {

        QueryWrapper<Room> qw = new QueryWrapper<>();
        if (StringUtils.isNotBlank(campus)) qw.eq("campus", campus);
        if (StringUtils.isNotBlank(building)) qw.eq("building", building);
        if (onlyOpen != null && onlyOpen == 1) qw.eq("status", "open");

        if (StringUtils.isNotBlank(keyword)) {
            String k = keyword.trim();
            qw.and(w -> w.like("room_name", k)
                    .or().like("building", k)
                    .or().like("campus", k)
                    .or().like("remark", k));
        }
        qw.orderByAsc("campus").orderByAsc("building").orderByAsc("room_name");

        List<Room> rooms = roomService.list(qw);
        if (rooms == null || rooms.isEmpty()) {
            return Result.success(Collections.emptyList(), 0L);
        }

        // 当前占用：今天 + 当前时刻（reserved/checked_in/late 且时间覆盖 now）
        List<Integer> roomIds = rooms.stream().map(Room::getId).filter(Objects::nonNull).collect(Collectors.toList());
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        Map<Integer, Integer> usedMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = reservationMapper.selectOccupiedSeatCountByRoomIds(roomIds, today, now);
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    Object ridObj = r.get("roomId");
                    Object cntObj = r.get("cnt");
                    if (ridObj == null) continue;
                    Integer rid = Integer.valueOf(String.valueOf(ridObj));
                    Integer cnt = (cntObj == null) ? 0 : Integer.valueOf(String.valueOf(cntObj));
                    usedMap.put(rid, cnt);
                }
            }
        } catch (Exception ignore) {
        }

        List<AdminRoomRowVO> list = rooms.stream().map(r -> {
            AdminRoomRowVO vo = new AdminRoomRowVO();
            vo.setId(r.getId());
            vo.setCampus(r.getCampus());
            vo.setBuilding(r.getBuilding());
            vo.setRoomName(r.getRoomName());
            vo.setCapacity(r.getTotalSeats());
            vo.setOpenSeats(r.getOpenSeats());
            vo.setStatus(r.getStatus());
            vo.setFloor(parseFloor(r.getRoomName()));
            vo.setName(buildDisplayName(vo.getFloor(), r.getRoomName()));
            vo.setUsedSeats(usedMap.getOrDefault(r.getId(), 0));
            return vo;
        }).collect(Collectors.toList());

        return Result.success(list, (long) list.size());
    }

    @PostMapping("/create")
    public Result create(@RequestBody CreateRoomDTO dto) {
        if (dto == null
                || StringUtils.isBlank(dto.getCampus())
                || StringUtils.isBlank(dto.getBuilding())
                || StringUtils.isBlank(dto.getRoomName())
                || dto.getTotalSeats() == null
                || dto.getTotalSeats() <= 0) {
            return Result.fail("参数不完整");
        }

        QueryWrapper<Room> existsQ = new QueryWrapper<>();
        existsQ.eq("campus", dto.getCampus())
                .eq("building", dto.getBuilding())
                .eq("room_name", dto.getRoomName());
        if (roomService.count(existsQ) > 0) {
            return Result.fail("该自习室已存在");
        }

        Room room = new Room();
        room.setCampus(dto.getCampus().trim());
        room.setBuilding(dto.getBuilding().trim());
        room.setRoomName(dto.getRoomName().trim());
        room.setTotalSeats(dto.getTotalSeats());
        room.setOpenSeats(dto.getTotalSeats());
        room.setStatus("open");
        room.setRemark(dto.getRemark());

        boolean ok = roomService.save(room);
        if (!ok || room.getId() == null) {
            return Result.fail("创建失败");
        }

        // 生成座位：默认 8 列，可通过 dto.cols 覆盖
        final int cols = (dto.getCols() != null && dto.getCols() > 0) ? dto.getCols() : 8;
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= dto.getTotalSeats(); i++) {
            int row = (i - 1) / cols + 1;
            int col = (i - 1) % cols + 1;
            Seat seat = new Seat();
            seat.setRoomId(room.getId());
            seat.setSeatNo(row + "-" + col);
            seat.setStatus("enabled");
            seats.add(seat);
        }
        seatService.saveBatch(seats);

        return Result.success(room.getId());
    }

    @PutMapping("/status")
    public Result changeStatus(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("roomId") == null || body.get("status") == null) {
            return Result.fail("缺少参数");
        }
        Integer roomId = Integer.valueOf(String.valueOf(body.get("roomId")));
        String status = String.valueOf(body.get("status"));
        if (!Arrays.asList("open", "closed", "maintain").contains(status)) {
            return Result.fail("非法状态");
        }
        Room upd = new Room();
        upd.setId(roomId);
        upd.setStatus(status);
        boolean ok = roomService.updateById(upd);
        return ok ? Result.success(true) : Result.fail("更新失败");
    }

    @GetMapping("/export/global")
    public ResponseEntity<byte[]> exportGlobal() {
        List<Room> rooms = roomService.list(new QueryWrapper<Room>()
                .orderByAsc("campus").orderByAsc("building").orderByAsc("room_name"));

        StringBuilder sb = new StringBuilder();
        sb.append("room_id,campus,building,room_name,total_seats,open_seats,status,remark\r\n");
        if (rooms != null) {
            for (Room r : rooms) {
                sb.append(r.getId() == null ? "" : r.getId()).append(',')
                        .append(csv(r.getCampus())).append(',')
                        .append(csv(r.getBuilding())).append(',')
                        .append(csv(r.getRoomName())).append(',')
                        .append(r.getTotalSeats() == null ? "" : r.getTotalSeats()).append(',')
                        .append(r.getOpenSeats() == null ? "" : r.getOpenSeats()).append(',')
                        .append(csv(r.getStatus())).append(',')
                        .append(csv(r.getRemark())).append("\r\n");
            }
        }

        byte[] utf8 = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = withBom(utf8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rooms_global.csv");
        return ResponseEntity.ok().headers(headers).body(out);
    }

    @GetMapping("/export/roomSeats")
    public ResponseEntity<byte[]> exportRoomSeats(@RequestParam("roomId") Integer roomId) {
        Room room = (roomId == null) ? null : roomService.getById(roomId);
        if (room == null) {
            byte[] out = withBom("error: room not found\r\n".getBytes(StandardCharsets.UTF_8));
            return ResponseEntity.badRequest().body(out);
        }

        List<Seat> seats = seatService.list(new QueryWrapper<Seat>()
                .eq("room_id", roomId)
                .orderByAsc("seat_no"));

        StringBuilder sb = new StringBuilder();
        sb.append("room_id,room_name,seat_id,seat_no,status,remark\r\n");
        if (seats != null) {
            for (Seat s : seats) {
                sb.append(roomId).append(',')
                        .append(csv(room.getRoomName())).append(',')
                        .append(s.getId() == null ? "" : s.getId()).append(',')
                        .append(csv(s.getSeatNo())).append(',')
                        .append(csv(s.getStatus())).append(',')
                        .append(csv(s.getRemark())).append("\r\n");
            }
        }

        byte[] utf8 = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = withBom(utf8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=room_" + roomId + "_seats.csv");
        return ResponseEntity.ok().headers(headers).body(out);
    }

    // ================= helpers =================

    private static byte[] withBom(byte[] utf8) {
        // UTF-8 BOM: EF BB BF
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] out = new byte[bom.length + utf8.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(utf8, 0, out, bom.length, utf8.length);
        return out;
    }

    /** 简易 CSV 转义：遇到逗号/换行/引号则整体加引号，引号变成双引号 */
    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private static Integer parseFloor(String roomName) {
        if (roomName == null) return null;
        String digits = roomName.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits.substring(0, 1));
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildDisplayName(Integer floor, String roomName) {
        if (roomName == null) return "";
        if (floor == null) return roomName;
        return floor + "楼 " + roomName + " 自习室";
    }
}
