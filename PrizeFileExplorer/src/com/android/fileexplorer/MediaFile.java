/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.fileexplorer;

import android.mtp.MtpConstants;
import android.media.DecoderCapabilities;
import android.media.DecoderCapabilities.VideoDecoder;
import android.media.DecoderCapabilities.AudioDecoder;

import java.util.HashMap;
import java.util.List;
public class MediaFile {

    // Audio file types
    public static final int FILE_TYPE_MP3     = 1;
    public static final int FILE_TYPE_M4A     = 2;
    public static final int FILE_TYPE_WAV     = 3;
    public static final int FILE_TYPE_AMR     = 4;
    public static final int FILE_TYPE_AWB     = 5;
    public static final int FILE_TYPE_WMA     = 6;
    public static final int FILE_TYPE_OGG     = 7;
    public static final int FILE_TYPE_AAC     = 8;
    public static final int FILE_TYPE_MKA     = 9;
    public static final int FILE_TYPE_FLAC    = 10;
    private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
    private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC;

    // More audio file types
    public static final int FILE_TYPE_DTS   = 300;
    public static final int FILE_TYPE_3GPA  = 301;
    public static final int FILE_TYPE_AC3   = 302;
    public static final int FILE_TYPE_QCP   = 303;
    public static final int FILE_TYPE_PCM   = 304;
    public static final int FILE_TYPE_EC3   = 305;
    private static final int FIRST_AUDIO_FILE_TYPE_EXT = FILE_TYPE_DTS;
    private static final int LAST_AUDIO_FILE_TYPE_EXT = FILE_TYPE_EC3;

    // MIDI file types
    public static final int FILE_TYPE_MID     = 11;
    public static final int FILE_TYPE_SMF     = 12;
    public static final int FILE_TYPE_IMY     = 13;
    private static final int FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID;
    private static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;
   
    // Video file types
    public static final int FILE_TYPE_FLV     = 20;
    public static final int FILE_TYPE_MP4     = 21;
    public static final int FILE_TYPE_M4V     = 22;
    public static final int FILE_TYPE_3GPP    = 23;
    public static final int FILE_TYPE_3GPP2   = 24;
    public static final int FILE_TYPE_WMV     = 25;
    public static final int FILE_TYPE_ASF     = 26;
    public static final int FILE_TYPE_MKV     = 27;
    public static final int FILE_TYPE_MP2TS   = 28;
    public static final int FILE_TYPE_AVI     = 29;
    public static final int FILE_TYPE_WEBM    = 30;
    public static final int FILE_TYPE_MOV     = 52;
    private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_FLV;
    private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_WEBM;
    
    // More video file types
    public static final int FILE_TYPE_MP2PS   = 200;
    public static final int FILE_TYPE_DIVX    = 201;
    private static final int FIRST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS;
    private static final int LAST_VIDEO_FILE_TYPE2 = FILE_TYPE_DIVX;

    // Image file types
    public static final int FILE_TYPE_JPEG    = 31;
    public static final int FILE_TYPE_GIF     = 32;
    public static final int FILE_TYPE_PNG     = 33;
    public static final int FILE_TYPE_BMP     = 34;
    public static final int FILE_TYPE_WBMP    = 35;
    public static final int FILE_TYPE_WEBP    = 36;
    private static final int FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG;
    private static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WEBP;
   
    // Playlist file types
    public static final int FILE_TYPE_M3U      = 41;
    public static final int FILE_TYPE_PLS      = 42;
    public static final int FILE_TYPE_WPL      = 43;
    public static final int FILE_TYPE_HTTPLIVE = 44;
    public static final int FILE_TYPE_DASH     = 45;

    private static final int FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U;
    private static final int LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_DASH;

    // Drm file types
    public static final int FILE_TYPE_FL      = 51;
    private static final int FIRST_DRM_FILE_TYPE = FILE_TYPE_FL;
    private static final int LAST_DRM_FILE_TYPE = FILE_TYPE_FL;

    // Other popular file types
    public static final int FILE_TYPE_TEXT          = 100;
    public static final int FILE_TYPE_HTML          = 101;
    public static final int FILE_TYPE_PDF           = 102;
    public static final int FILE_TYPE_XML           = 103;
    public static final int FILE_TYPE_MS_WORD       = 104;
    public static final int FILE_TYPE_MS_EXCEL      = 105;
    public static final int FILE_TYPE_MS_POWERPOINT = 106;
    public static final int FILE_TYPE_ZIP           = 107;

    public static class MediaFileType {
        public final int fileType;
        public final String mimeType;

        MediaFileType(int fileType, String mimeType) {
            this.fileType = fileType;
            this.mimeType = mimeType;
        }
    }

    private static HashMap<String, MediaFileType> sFileTypeMap = new HashMap<String, MediaFileType>();
    private static HashMap<String, Integer> sMimeTypeMap = new HashMap<String, Integer>();

    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
        sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
    }

    static void addFileType(String extension, int fileType, String mimeType, int mtpFormatCode) {
        addFileType(extension, fileType, mimeType);
    }

    static {
    	addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3);
        addFileType("MP3D", FILE_TYPE_MP3, "audio/mp3d", MtpConstants.FORMAT_MP3);
        addFileType("MPGA", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3);
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("M4A", FILE_TYPE_M4A, "audio/m4a", MtpConstants.FORMAT_MPEG);
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4a-latm", MtpConstants.FORMAT_MPEG);
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav", MtpConstants.FORMAT_WAV);
        addFileType("WAV", FILE_TYPE_WAV, "audio/wav", MtpConstants.FORMAT_WAV);
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
        if (isWMAEnabled()) {
            addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma", MtpConstants.FORMAT_WMA);
        }
        addFileType("OGG", FILE_TYPE_OGG, "audio/ogg", MtpConstants.FORMAT_OGG);
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("OGA", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac", MtpConstants.FORMAT_AAC);
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac-adts", MtpConstants.FORMAT_AAC);
        addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska");
        addFileType("3G2", FILE_TYPE_3GPP2, "audio/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
 
        addFileType("MID", FILE_TYPE_MID, "audio/midi");
        addFileType("MIDI", FILE_TYPE_MID, "audio/midi");
        addFileType("XMF", FILE_TYPE_MID, "audio/midi");
        addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
        addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
        addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");
        addFileType("RTX", FILE_TYPE_MID, "audio/midi");
        addFileType("OTA", FILE_TYPE_MID, "audio/midi");
        addFileType("MXMF", FILE_TYPE_MID, "audio/midi");
        
        addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
        addFileType("MPG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
        addFileType("MP4", FILE_TYPE_MP4, "video/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("MOV", FILE_TYPE_MOV, "video/quicktime", MtpConstants.FORMAT_MPEG);
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp",  MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska");
        addFileType("WEBM", FILE_TYPE_WEBM, "video/webm");
        addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts");
        addFileType("AVI", FILE_TYPE_AVI, "video/avi");
        addFileType("FLV", FILE_TYPE_FLV, "video/flv");

        if (isWMVEnabled()) {
            addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv", MtpConstants.FORMAT_WMV);
            addFileType("ASF", FILE_TYPE_ASF, "video/x-ms-asf");
        }

        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("GIF", FILE_TYPE_GIF, "image/gif", MtpConstants.FORMAT_GIF);
        addFileType("PNG", FILE_TYPE_PNG, "image/png", MtpConstants.FORMAT_PNG);
        addFileType("BMP", FILE_TYPE_BMP, "image/bmp", MtpConstants.FORMAT_BMP);
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp", MtpConstants.FORMAT_BMP);
        addFileType("BMP", FILE_TYPE_BMP, "image/x-MS-bmp", MtpConstants.FORMAT_BMP);
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");
        addFileType("WEBP", FILE_TYPE_WEBP, "image/webp");
 
        addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
        addFileType("M3U", FILE_TYPE_M3U, "application/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
        addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls", MtpConstants.FORMAT_PLS_PLAYLIST);
        addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl", MtpConstants.FORMAT_WPL_PLAYLIST);
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "application/vnd.apple.mpegurl");
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/mpegurl");
        addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/x-mpegurl");
        addFileType("MPD", FILE_TYPE_DASH, "application/dash+xml");

        addFileType("FL", FILE_TYPE_FL, "application/x-android-drm-fl");

        addFileType("TXT", FILE_TYPE_TEXT, "text/plain", MtpConstants.FORMAT_TEXT);
        addFileType("HTM", FILE_TYPE_HTML, "text/html", MtpConstants.FORMAT_HTML);
        addFileType("HTML", FILE_TYPE_HTML, "text/html", MtpConstants.FORMAT_HTML);
        addFileType("PDF", FILE_TYPE_PDF, "application/pdf");
        addFileType("DOC", FILE_TYPE_MS_WORD, "application/msword", MtpConstants.FORMAT_MS_WORD_DOCUMENT);
        addFileType("XLS", FILE_TYPE_MS_EXCEL, "application/vnd.ms-excel", MtpConstants.FORMAT_MS_EXCEL_SPREADSHEET);
        addFileType("PPT", FILE_TYPE_MS_POWERPOINT, "application/mspowerpoint", MtpConstants.FORMAT_MS_POWERPOINT_PRESENTATION);
        addFileType("FLAC", FILE_TYPE_FLAC, "audio/flac", MtpConstants.FORMAT_FLAC);
        addFileType("ZIP", FILE_TYPE_ZIP, "application/zip");
        addFileType("MPG", FILE_TYPE_MP2PS, "video/mp2p");
        addFileType("MPEG", FILE_TYPE_MP2PS, "video/mp2p");
        addFileType("DIVX", FILE_TYPE_DIVX, "video/divx");
        addFileType("QCP", FILE_TYPE_QCP, "audio/qcelp");
        addFileType("AC3", FILE_TYPE_AC3, "audio/ac3");
        addFileType("EC3", FILE_TYPE_EC3, "audio/eac3");
    }

    public static boolean isAudioFileType(int fileType) {
        return ((fileType >= FIRST_AUDIO_FILE_TYPE && fileType <= LAST_AUDIO_FILE_TYPE) || (fileType >= FIRST_MIDI_FILE_TYPE && fileType <= LAST_MIDI_FILE_TYPE));
    }

    public static boolean isVideoFileType(int fileType) {
        return (fileType >= FIRST_VIDEO_FILE_TYPE && fileType <= LAST_VIDEO_FILE_TYPE);
    }

    public static boolean isImageFileType(int fileType) {
        return (fileType >= FIRST_IMAGE_FILE_TYPE && fileType <= LAST_IMAGE_FILE_TYPE);
    }

    public static MediaFileType getFileType(String path) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0)
            return null;
        return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
    }
    private static boolean isWMAEnabled() {
        List<AudioDecoder> decoders = DecoderCapabilities.getAudioDecoders();
        int count = decoders.size();
        for (int i = 0; i < count; i++) {
            AudioDecoder decoder = decoders.get(i);
            if (decoder == AudioDecoder.AUDIO_DECODER_WMA) {
                return true;
            }
        }
        return false;
    }
    private static boolean isWMVEnabled() {
        List<VideoDecoder> decoders = DecoderCapabilities.getVideoDecoders();
        int count = decoders.size();
        for (int i = 0; i < count; i++) {
            VideoDecoder decoder = decoders.get(i);
            if (decoder == VideoDecoder.VIDEO_DECODER_WMV) {
                return true;
            }
        }
        return false;
    }
    /*PRIZE-type-xiaxuefeng-2015-4-1-start*/
    //根据视频文件路径判断文件类型
    public static boolean isVideoFileType(String path) {  
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isVideoFileType(type.fileType);
        }
        return false;
    }
    //根据音频文件路径判断文件类型
    public static boolean isAudioFileType(String path) {  
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isAudioFileType(type.fileType);
        }
        return false;
    }
    //根据图片文件路径判断文件类型
    public static boolean isImageFileType(String path) {  
        MediaFileType type = getFileType(path);
        if(null != type) {
            return isImageFileType(type.fileType);
        }
        return false;
    }
    /*PRIZE-type-xiaxuefeng-2015-4-1-end*/
}
