package mq;

import com.halfhuman.entry.InteractContextFactory;
import com.halfhuman.service.IInteractContextSender;
import llc.model.ModuleUse;
import llc.service.ServiceHelper;
import mq.Enums.EnvironmentType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Cons;

import static com.halfhuman.entry.InteractContextFactory.getIcFetcher;

/**
 * 接受log
 * Created by hxx on 5/3/16.
 */
class Receiver {
    static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    public static void handleMessage(String slog, String environment) {
        NormalLog log;
        try {
            log = new NormalLog(slog);
        }catch (Exception e){
            logger.error("Parse log error: " + e.getMessage());
            return;
        }
        EnvironmentType envType = log.getEnvType();
        String modTrans = log.getModtrans();
        // 部署在alpha服务器上才存入sql数据库
        if (environment.equals("alpha")) {
            if (envType.name().toLowerCase().equals("alpha")) {
                if (modTrans.contains("->")){   //给宇骁上下文服务器
                    try{
                        InteractContextFactory.setVersion("alpha");
                        IInteractContextSender icsender = getIcFetcher();
                        boolean res = icsender.recordToContext(slog);
                        logger.info("上下文发送内容: " + slog);
                        logger.info("上下文服务器发送结果 "+res);
                    }catch (Exception e){
                        e.printStackTrace();
                        logger.error("上下文发送失败, 原因:" + e.getMessage());
                    }

                }
            }

            if (envType.equals(EnvironmentType.Online) || envType.equals(EnvironmentType.Release)) {
                if (!modTrans.equals("FrontEnd")) {
                    try {
                        ServiceHelper.getUserService().insertHourCache(log);
                    } catch (Exception e) {
                        logger.error("Error occurs! throws: " + e.getMessage());
                    }
                }
            }
            if (log.containMethodName() && !modTrans.equals("FrontEnd") && !modTrans.equals("FlashGame")
                    && !modTrans.equals("dialogtree") && !modTrans.equals("openSDK")
                    && !modTrans.equals("controller") && !modTrans.equals("protoss")) {
                String methodName = log.getMethodName();
                if (!methodName.equals("") &&
                        !Cons.uselessMethod.contains(methodName)){
                    String usedTime = log.getUsedTime();
                    String env = envType.name().toLowerCase();
                    String tableName;
                    if (env.equals("online") || env.equals("release")){
                        tableName = "stat_module";
                    }else{
                        tableName = env + "_stat_module";
                    }
                    String memberId = log.getMember_id();
                    String logTime = log.getLog_time();
                    DateTime dateTime = new DateTime(Long.parseLong(logTime));
                    String sDate = dateTime.toString("yyyy-MM-dd HH:mm:ss");
                    String module = log.getModtrans();
                    String content = log.getContent();
                    ModuleUse moduleUse = new ModuleUse(memberId, sDate, module, usedTime, content);
                    try{
                        if (env.equals("alpha")) {
                            ServiceHelper.getUserService().insertUsedTime_alpha(moduleUse);
                        } else if(env.equals("beta")){
                            ServiceHelper.getUserService().insertUsedTime_beta(moduleUse);
                        }else{
                            ServiceHelper.getUserService().insertUsedTime(moduleUse);
                        }
                        logger.info("Insert into " + tableName + " successfully");
                    }catch (Exception e){
                        logger.error("Insert error: " + e.getMessage());
                    }
                }
            }
        } else {
            if (envType.name().toLowerCase().equals("release")) {
                if (modTrans.contains("->")){   //给宇骁上下文服务器
                    try{
                        InteractContextFactory.setVersion("release");
                        IInteractContextSender icsender = getIcFetcher();
                        boolean res = icsender.recordToContext(slog);
                        logger.info("上下文发送内容: " + slog);
                        logger.info("上下文服务器发送结果 "+res);
                    }catch (Exception e){
                        e.printStackTrace();
                        logger.error("上下文发送失败, 原因:" + e.getMessage());
                    }

                }
            }
        }
    }

}