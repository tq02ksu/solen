//package top.fengpingtech.solen;
//
//import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import java.util.Date;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
///**
// * 基准加增量的方式同步数据，
// * 数据库里保存全量数据的同时，记录全量数据的时间点（增量同步的进度点）。
// *  应用程序启动后，开启一个线程，每三秒执行一次增量同步。
// *  同步时有以下三种情况：
// *  1。 进度点查不到：先保存开始时间，拉全量，把开始时间保存为进度点。
// *  2。 进度点查到了，但是离当前时间太远了。（超过一天）。 处理方式跟1一样。
// *  3. 进度点查到了，每次同步一小时的数据，直到追上增量。
// *
// *  增量处理逻辑：
// *  内存里开一个集合，保存处理过的事件key，此KEY由三部分组成，设备ID, 消息类型，数据类型。
// *  处理消息之前与这个消息做一个比较，存在的消息直接跳过，不存在的处理之后加到集合。
// *
// */
//public abstract class DeltaSync {
//    private static final Long ONE_DAY_IN_MILLS = 86400L * 1000;
//
//    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory(){
//        {
//            setDaemon(true);
//            setThreadNamePrefix("DeltaSync-");
//        }
//    });
//
//
//
//    @PostConstruct
//    public void init() {
//        executorService.scheduleWithFixedDelay(this::sync, 1, 3, TimeUnit.SECONDS);
//    }
//
//    @PreDestroy
//    public void destroy() {
//        executorService.shutdownNow();
//    }
//
//    private void sync() {
//        Date savePoint = querySavePoint();
//        // 先处理 1, 2 情况
//        if (savePoint == null || System.currentTimeMillis() - savePoint.getTime() > ONE_DAY_IN_MILLS ) {
//            // 保存开始时间
//            savePoint = new Date();
//            // 全量数据写入
//            importAllDeviceStatus();
//            // 保存
//            saveSavePoint(savePoint);
//        } else {
//            syncDelta(savePoint);
//        }
//    }
//
//    private void syncDelta(Date savePoint) {
//        long maxDuration = 3600L * 1000L;
//        // 尝试每次最多追一小时数据。直到进度点与系统时间相差3秒以内。
//        for (Date endTime = new Date(Long.min(savePoint.getTime() + maxDuration, System.currentTimeMillis()));
//             System.currentTimeMillis() - savePoint.getTime() < 3000; savePoint = endTime ) {
//            // TODO 处理[savePoint, endTime) 之间的增量
//            processDelta(savePoint, endTime);
//            saveSavePoint(endTime);
//        }
//    }
//
//    protected void processDelta(Date startTime, Date endTime) {
//        // 为了避免重复处理，采用一个hashSet保存处理过的数据，
//        Set<String> flags = new HashSet<>();
//        int pageNo = 1, pageSize = 300;
//        for (List<Event> events = queryEvents(startTime, endTime, pageNo, pageSize);
//             events != null && !events.isEmpty() ;
//             events = events.size() < pageSize ? null : queryEvents(startTime, endTime, ++pageNo, pageSize)) {
//            for (Event event : events) {
//                switch (event.getType()) {
//                    case MESSAGE_RECEIVING: {
//                        // 处理串口消息, 按info_type 处理。
//                        String content = event.getDetails().get("content");
//                        Matcher matcher = Pattern.compile("^.*info_type\":\"(\\d+)\".*").matcher(content);
//                        if (matcher.matches()) {
//                            String infoType = matcher.group(1);
//                            String key = String.format("%s-%s-%s", event.getEventId(), event.getType(), infoType);
//                            if (!flags.contains(key)) {
//                                flags.add(key);
//                                saveSerialData(event.getDeviceId(), event.getDetails().get("content"));
//                            }
//                        }
//                        break;
//                    }
//                    case ATTRIBUTE_UPDATE: {
//                        // 处理状态变化, 按字段来更新，每个字段存为一个flag, 避免处理到旧数据。
//                        Map<String, String> keys = event.getDetails().keySet()
//                                .stream()
//                                .collect(Collectors.toMap(
//                                        k -> k,
//                                        k -> String.format("%s-%s-%s", event.getEventId(), event.getType(), k)));
//                        if (!flags.containsAll(keys.values())) {
//                            Device device = getDevice(event.getEventId());
//                            // TODO set event value.
//                            for (Map.Entry<String, String> entry : event.getDetails().entrySet()) {
//                                String key = keys.get(entry.getKey());
//                                if (!flags.contains(key)) {
//                                    flags.add(keys.get(key));
//                                    String value = entry.getValue();
//                                    // 设备值，如果不是字符串，需要转换一下数据类型
//                                }
//                            }
//                            saveDevice(device);
//                        }
//                        break;
//                    }
//                    default:
//                        // to nothing
//                }
//            }
//        }
//    }
//
//    protected abstract void saveDevice(Device device);
//
//    protected abstract Device getDevice(Long eventId);
//
//    protected abstract void saveSerialData(String deviceId, String content);
//
//    protected abstract List<Event> queryEvents(Date startTime, Date endTime, int pageNo, int pageSize);
//
//    private void importAllDeviceStatus() {
//        int pageNo = 1, pageSize = 300;
//        for (Page<Device> page = queryDeviceList(pageNo, pageSize);
//              page != null && page.getList() != null && !page.getList().isEmpty();
//              page = page.getTotal() > (pageNo * pageSize) ? queryDeviceList(++pageNo, pageSize) : null) {
//            // TODO import page data to db
//        }
//    }
//
//    protected abstract Page<Device> queryDeviceList(int pageNo, int pageSize);
//
//    abstract Date querySavePoint();
//    abstract void saveSavePoint(Date savePoint);
//
//    public static void main(String[] args) throws Exception {
//        String json = "{\"IP_ADDRESS\":\"\",\"MAC_ADDRESS\":\"\",\"SESSION\":\"\",\"reserve\":\"\",\"DEVICE_ID\":\"\",\"info_type\":\"2\",\"body\":{\"CPU_USE_INFO\":\"35%\",\"MEM_AVAI_INFO\":\"2.1G\",\"STORAGE_AVAI_INFO\":\"101G\",\"STATUS_CODE\":\"00\",\"devicelist\":[{\"DEVICE_ID\":\"1028\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"},{\"DEVICE_ID\":\"1025\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"},{\"DEVICE_ID\":\"1021\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"},{\"DEVICE_ID\":\"1027\",\"STATUS_CODE\":\"0\",\"STATUS_REASON_CODE\":\"\"}]}}";
//        Matcher matcher = Pattern.compile("^.*info_type\":\"(\\d+)\".*").matcher(json);
//        if (matcher.matches()) {
//            System.out.println(matcher.group(1));
//        }
//    }
//}
//
//interface Page<T> {
//    int getTotal();
//    List<T> getList();
//}
//
//interface Device {
//}
