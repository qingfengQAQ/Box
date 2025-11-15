package me.nullpoint.mod.commands.impl;

import me.nullpoint.api.managers.CommandManager;
import me.nullpoint.mod.commands.Command;

import java.text.DecimalFormat;
import java.util.List;

public class VClipCommand extends Command {

	public VClipCommand() {
		super("vclip", "vclip", "[range]");
	}

	@Override
	public void runCommand(String[] parameters) {
		if (parameters.length != 1) {
			sendUsage();
			return;
		}

		double distance;
		try {
			// 解析用户输入的距离值
			distance = Double.parseDouble(parameters[0]);

			// 检查距离是否在允许范围内
			if (distance < -6 || distance > 6) {
				CommandManager.sendChatMessage("§c[x] Please enter a value between \"3\" and \"3\".");
				return;
			}
		} catch (NumberFormatException e) {
			CommandManager.sendChatMessage("§c[!] Invalid values.");
			return;
		}

		// 获取玩家当前位置
		double x = mc.player.getX();
		double y = mc.player.getY();
		double z = mc.player.getZ();

		// 应用垂直位移
		double newY = y + distance;

		// 执行传送
		mc.player.setPosition(x, newY, z);

		// 发送成功消息
		DecimalFormat df = new DecimalFormat("0.0");
		CommandManager.sendChatMessage("§a [√] PullUp execution is complete!");
	}

	@Override
	public String[] getAutocorrect(int count, List<String> seperated) {
		// 提供可能的自动补全建议
		return new String[]{"1.5", "2", "-1", "3.5", "-2.5"};
	}
}