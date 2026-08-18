package dev.easytrade.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public record ScreenPos(float x, float y) {

	public static ScreenPos project(Minecraft mc, Vec3 worldPos) {
		Camera cam = mc.gameRenderer.mainCamera();
		Matrix4f mvp = cam.getViewRotationProjectionMatrix(new Matrix4f());
		Vec3 rel = worldPos.subtract(cam.position());
		Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f).mul(mvp);
		if (clip.w <= 0.01f) {
			return null;
		}
		float ndcX = clip.x / clip.w;
		float ndcY = clip.y / clip.w;
		if (ndcX < -1.5f || ndcX > 1.5f || ndcY < -1.5f || ndcY > 1.5f) {
			return null;
		}
		int gw = mc.getWindow().getGuiScaledWidth();
		int gh = mc.getWindow().getGuiScaledHeight();
		float sx = (ndcX * 0.5f + 0.5f) * gw;
		float sy = (0.5f - ndcY * 0.5f) * gh;
		return new ScreenPos(sx, sy);
	}
}