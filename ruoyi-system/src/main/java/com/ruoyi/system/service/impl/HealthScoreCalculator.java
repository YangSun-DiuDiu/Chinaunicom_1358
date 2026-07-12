package com.ruoyi.system.service.impl;

/**
 * 网络健康评分算法（三维加权，纯函数可测）
 * 设备40% + 线路30% + 流量30%
 *
 * @author ruoyi
 */
public class HealthScoreCalculator
{
    public static class ScoreResult
    {
        public double deviceScore;
        public double lineScore;
        public double flowScore;
        public double totalScore;
        public String grade;
    }

    /**
     * 计算网络健康评分
     *
     * @param onlineRate    在线率 0-1
     * @param faultCount    故障次数
     * @param deviceCount   设备总数
     * @param portUpRate    端口UP率 0-1
     * @param avgPacketLoss 平均丢包率 0-1
     * @param bwOverRatio   带宽超阈值占比 0-1
     * @param flowJitter    流量抖动系数 0-1
     * @return 三维评分 + 总分 + 评级
     */
    public static ScoreResult calculate(double onlineRate, int faultCount, int deviceCount,
                                        double portUpRate, double avgPacketLoss,
                                        double bwOverRatio, double flowJitter)
    {
        double faultRatio = deviceCount > 0 ? Math.min((double) faultCount / deviceCount, 1.0) : 0.0;
        double device = (onlineRate * 0.6 + (1 - faultRatio) * 0.4) * 100;
        double line = (portUpRate * 0.5 + (1 - avgPacketLoss) * 0.5) * 100;
        double flow = ((1 - bwOverRatio) * 0.7 + (1 - flowJitter) * 0.3) * 100;
        double total = device * 0.4 + line * 0.3 + flow * 0.3;

        ScoreResult r = new ScoreResult();
        r.deviceScore = round2(device);
        r.lineScore = round2(line);
        r.flowScore = round2(flow);
        r.totalScore = round2(total);
        r.grade = grade(total);
        return r;
    }

    /**
     * 健康评级：优≥90 / 良75-89 / 中60-74 / 差<60
     */
    public static String grade(double score)
    {
        if (score >= 90) return "优";
        if (score >= 75) return "良";
        if (score >= 60) return "中";
        return "差";
    }

    private static double round2(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }
}
