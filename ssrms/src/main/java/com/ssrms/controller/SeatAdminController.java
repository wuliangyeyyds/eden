package com.ssrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ssrms.common.Result;
import com.ssrms.controller.dto.SeatIdsDTO;
import com.ssrms.controller.vo.AdminRoomRowVO;
import com.ssrms.controller.vo.SeatLayoutVO;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/seat/admin")
public class SeatAdminController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private ReservationMapper reservationMapper;

    private static final Pattern RC = Pattern.compile("^(\\d+)-(\\d+)$");

    // =======================
    // 兼容接口（解决前端 404）
    // 说明：
    // - 你原本的规划里“校区/楼栋/房间列表/导出”在 /room/admin 下
    // - 但前端部分实现会习惯写成 /seat/admin/xxx
    // 为了避免来回改前端，这里给 /seat/admin 做一层“别名”接口。
    // =======================

    /** 校区下拉（别名）：GET /seat/admin/campuses */
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

    /** 楼栋下拉（别名）：GET /seat/admin/buildings?campus=xxx */
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

    /**
     * 房间列表（别名）：GET /seat/admin/rooms
     * 支持：campus/building/onlyOpen/keyword
     */
    @GetMapping("/rooms")
    public Result rooms(@RequestParam(value = "campus", required = false) String campus,
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

    /** 导出全局房间配置（别名）：GET /seat/admin/export/global */
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
        byte[] out = withBom(sb.toString().getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rooms_global.csv");
        return ResponseEntity.ok().headers(headers).body(out);
    }

    /** 导出当前教室座位配置（别名）：GET /seat/admin/export/roomSeats?roomId=xx */
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

        byte[] out = withBom(sb.toString().getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=room_" + roomId + "_seats.csv");
        return ResponseEntity.ok().headers(headers).body(out);
    }

    // =======================
    // 正式座位接口
    // =======================

    /**
     * 动态座位布局：从 seat 表读取并按 row 分组。
     * - 支持 seat_no 为 "r-c"（例：1-1）
     * - 支持 seat_no 为数字编码 row*100+col（例：101=>1-1, 505=>5-5）
     * - 其它情况：按顺序 8 列（或 totalSeats<=8 时用 totalSeats）自动映射
     */
    @GetMapping("/layout")
    public Result layout(@RequestParam("roomId") Integer roomId) {
        Room room = (roomId == null) ? null : roomService.getById(roomId);
        if (room == null) return Result.fail("自习室不存在");

        List<Seat> seats = seatService.list(
                new QueryWrapper<Seat>().eq("room_id", roomId).orderByAsc("seat_no")
        );
        if (seats == null) seats = Collections.emptyList();

        // 当前占用座位：今天 + 当前时间（distinct seat_id）
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        Set<Integer> occupied = new HashSet<>();
        try {
            List<Integer> ids = reservationMapper.selectOccupiedSeatIdsNow(roomId, today, now);
            if (ids != null) occupied.addAll(ids);
        } catch (Exception ignore) {
        }

        // 列数猜测：默认 8；若总座位 <=8，则一行铺满
        int colsGuess = 8;
        if (room.getTotalSeats() != null && room.getTotalSeats() > 0 && room.getTotalSeats() <= 8) {
            colsGuess = room.getTotalSeats();
        }

        List<SeatItemExt> items = new ArrayList<>();
        int free = 0, occ = 0, disabled = 0;

        for (int idx = 0; idx < seats.size(); idx++) {
            Seat s = seats.get(idx);
            SeatItemExt it = new SeatItemExt();
            it.id = s.getId();
            it.seatNo = s.getSeatNo();

            RowCol rc = parseRowCol(s.getSeatNo());
            int row;
            int col;

            if (rc != null) {
                row = rc.row;
                col = rc.col;
            } else {
                // fallback：按序号映射
                int n = parseIntSafe(s.getSeatNo());
                if (n <= 0) n = idx + 1;
                row = (n - 1) / colsGuess + 1;
                col = (n - 1) % colsGuess + 1;
            }

            it.row = row;
            it.col = col;
            it.displayNo = row + "-" + col;

            if ("disabled".equalsIgnoreCase(s.getStatus())) {
                it.status = "disabled";
                disabled++;
            } else if (occupied.contains(s.getId())) {
                it.status = "occupied";
                occ++;
            } else {
                it.status = "free";
                free++;
            }

            items.add(it);
        }

        // group by row
        Map<Integer, List<SeatItemExt>> byRow = items.stream()
                .collect(Collectors.groupingBy(x -> x.row, TreeMap::new, Collectors.toList()));

        List<SeatLayoutVO.SeatRow> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<SeatItemExt>> e : byRow.entrySet()) {
            List<SeatItemExt> list = e.getValue();
            list.sort(Comparator.comparingInt(a -> a.col));

            SeatLayoutVO.SeatRow row = new SeatLayoutVO.SeatRow();
            row.setRowNo(e.getKey());

            List<SeatLayoutVO.SeatItem> seatVos = new ArrayList<>();
            for (SeatItemExt it : list) {
                SeatLayoutVO.SeatItem vo = new SeatLayoutVO.SeatItem();
                vo.setId(it.id);
                vo.setSeatNo(it.seatNo);
                vo.setDisplayNo(it.displayNo);
                vo.setStatus(it.status);
                seatVos.add(vo);
            }
            row.setSeats(seatVos);
            rows.add(row);
        }

        SeatLayoutVO vo = new SeatLayoutVO();
        SeatLayoutVO.RoomBrief rb = new SeatLayoutVO.RoomBrief();
        rb.setId(room.getId());
        rb.setCampus(room.getCampus());
        rb.setBuilding(room.getBuilding());
        rb.setRoomName(room.getRoomName());
        rb.setTotalSeats(room.getTotalSeats());
        rb.setOpenSeats(room.getOpenSeats());
        rb.setStatus(room.getStatus());
        vo.setRoom(rb);
        vo.setRows(rows);

        SeatLayoutVO.SeatStats stats = new SeatLayoutVO.SeatStats();
        stats.setFree(free);
        stats.setOccupied(occ);
        stats.setDisabled(disabled);
        vo.setStats(stats);

        return Result.success(vo);
    }

    /** 批量设置座位状态（enabled/disabled） */
    @PostMapping("/batchStatus")
    public Result batchStatus(@RequestBody SeatIdsDTO dto) {
        if (dto == null || dto.getSeatIds() == null || dto.getSeatIds().isEmpty()) {
            return Result.fail("缺少 seatIds");
        }
        String status = StringUtils.isBlank(dto.getStatus()) ? "disabled" : dto.getStatus().trim();
        if (!"enabled".equalsIgnoreCase(status) && !"disabled".equalsIgnoreCase(status)) {
            return Result.fail("非法 status（仅支持 enabled/disabled）");
        }

        boolean ok = seatService.update(
                new UpdateWrapper<Seat>()
                        .in("id", dto.getSeatIds())
                        .set("status", status.toLowerCase())
        );
        return ok ? Result.success(true) : Result.fail("更新失败");
    }

    /** 兼容：禁用座位（相当于 batchStatus=disabled） */
    @PostMapping("/disable")
    public Result disable(@RequestBody SeatIdsDTO dto) {
        if (dto == null) dto = new SeatIdsDTO();
        dto.setStatus("disabled");
        return batchStatus(dto);
    }

    // ================= helpers =================

    private static RowCol parseRowCol(String seatNo) {
        if (StringUtils.isBlank(seatNo)) return null;
        String s = seatNo.trim();

        // 1) r-c
        Matcher m = RC.matcher(s);
        if (m.matches()) {
            try {
                int row = Integer.parseInt(m.group(1));
                int col = Integer.parseInt(m.group(2));
                if (row > 0 && col > 0) return new RowCol(row, col);
            } catch (Exception ignore) {
            }
        }

        // 2) numeric: row*100 + col (101=>1-1)
        int n = parseIntSafe(s);
        if (n >= 100) {
            int row = n / 100;
            int col = n % 100;
            if (row > 0 && col > 0) return new RowCol(row, col);
        }

        return null;
    }

    private static int parseIntSafe(String s) {
        if (StringUtils.isBlank(s)) return -1;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
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

    private static class RowCol {
        final int row;
        final int col;
        RowCol(int row, int col) { this.row = row; this.col = col; }
    }

    private static class SeatItemExt {
        Integer id;
        String seatNo;
        String displayNo;
        int row;
        int col;
        String status;
    }
}
