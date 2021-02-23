package com.example.crypt;

import android.util.Log;

import com.jcraft.jsch.*;

public class Authorization extends Exception{

    public ChannelSftp Authorization(String user, String password, int port, String host){
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(user, host, port);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        try {
            session.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException e) {
            e.printStackTrace();
        };
        try {
            sftp.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
        return sftp;
    }

}