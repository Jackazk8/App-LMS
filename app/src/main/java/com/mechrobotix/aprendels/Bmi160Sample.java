package com.mechrobotix.aprendels;

/** BLE BIN20 v1: paquete binario de 20 bytes del ESP32-C3.
 * Acceleration includes gravity (g); gyro is degrees/s. Device and phone clocks differ.
 */
public final class Bmi160Sample {
    public final long sequence, deviceMs, receivedMs;
    public final double ax, ay, az, gx, gy, gz;
    private Bmi160Sample(long seq,long time,long received,double[] v) {
        sequence=seq;deviceMs=time;receivedMs=received;
        ax=v[0];ay=v[1];az=v[2];gx=v[3];gy=v[4];gz=v[5];
    }
    /** BLE BIN20 v1, little-endian: u32 seq, u32 ms, i16 ax ay az gx gy gz. */
    public static Bmi160Sample parseBle(byte[] packet, long receivedMs) {
        if(packet==null || packet.length!=20) return null;
        java.nio.ByteBuffer b=java.nio.ByteBuffer.wrap(packet).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        long seq=((long)b.getInt()) & 0xffffffffL;
        long time=((long)b.getInt()) & 0xffffffffL;
        double[] v=new double[6];
        for(int i=0;i<6;i++) v[i]=b.getShort()/(i<3?16384.0:16.4);
        return new Bmi160Sample(seq,time,receivedMs,v);
    }
    public double accelerationNorm() { return Math.sqrt(ax*ax+ay*ay+az*az); }
}
