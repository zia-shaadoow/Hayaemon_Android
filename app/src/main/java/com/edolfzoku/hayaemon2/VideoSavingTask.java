/*
 * SongSavingTask
 *
 * Copyright (c) 2019 Ryota Yamauchi. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.edolfzoku.hayaemon2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class VideoSavingTask extends AsyncTask<Integer, Integer, Integer> {
    private PlaylistFragment playlistFragment = null;
    private String strPathTo;
    private AlertDialog alert;
    private int nLength;
    private String strMP4Path;

    public VideoSavingTask(PlaylistFragment playlistFragment, String strPathTo, AlertDialog alert, int nLength)
    {
        this.playlistFragment = playlistFragment;
        this.strPathTo = strPathTo;
        this.alert = alert;
        this.nLength = nLength;
    }

    @Override
    protected Integer doInBackground(Integer... params)
    {
        MediaCodec codec = null;
        MediaMuxer mux = null;
        File inputFile = new File(strPathTo);
        File inputMp4File = null;
        FileInputStream fis = null;
        try {
            if (Build.VERSION.SDK_INT < 18) return 0;

            fis = new FileInputStream(inputFile);
            fis.skip(44);

            String strTempPath = playlistFragment.getActivity().getExternalCacheDir() + "/temp.mp4";
            inputMp4File = new File(strTempPath);
            if (inputMp4File.exists()) inputMp4File.delete();
            SeekableByteChannel out = NIOUtils.writableFileChannel(strTempPath);
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(out, Rational.R(1, 1));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeResource(playlistFragment.getResources(), R.drawable.cameraroll_jp, options);
            encoder.encodeImage(bitmap);
            encoder.encodeImage(bitmap);
            bitmap.recycle();
            bitmap = null;
            encoder.finish();
            NIOUtils.closeQuietly(out);

            ArrayList<SongItem> arSongs = playlistFragment.getArPlaylists().get(playlistFragment.getSelectedPlaylist());
            SongItem item = arSongs.get(playlistFragment.getSelectedItem());
            String strTitle = item.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
            strMP4Path = Environment.getExternalStorageDirectory() + "/" + strTitle + ".mp4";
            File outputFile = new File(strMP4Path);
            if (outputFile.exists()) {
                int i = 2;
                File fileForCheck;
                String strTemp = null;
                while (true) {
                    strTemp = Environment.getExternalStorageDirectory() + "/" + strTitle + String.format("%d", i) + ".mp4";
                    fileForCheck = new File(strTemp);
                    if (!fileForCheck.exists()) break;
                    i++;
                }
                strMP4Path = Environment.getExternalStorageDirectory() + "/" + strTitle + String.format("%d", i) + ".mp4";
                outputFile = new File(strMP4Path);
            }

            mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(strTempPath);
            videoExtractor.selectTrack(0);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);

            codec = MediaCodec.createEncoderByType("audio/mp4a-latm");
            MediaFormat outputFormat = new MediaFormat();
            outputFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            outputFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            outputFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1024);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] codecInputBuffers = null;
            ByteBuffer[] codecOutputBuffers = null;
            if(Build.VERSION.SDK_INT < 21) {
                codecInputBuffers = codec.getInputBuffers();
                codecOutputBuffers = codec.getOutputBuffers();
            }

            MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();
            byte[] tempBuffer = new byte[44100];
            boolean hasMoreData = true;
            double presentationTimeUs = 0;
            int audioTrackIdx = 0;
            int videoTrackIdx = 0;
            int totalBytesRead = 0;
            do {
                if(playlistFragment.isFinish()) break;
                int inputBufIndex = 0;
                while (inputBufIndex != -1 && hasMoreData) {
                    if(playlistFragment.isFinish()) break;
                    inputBufIndex = codec.dequeueInputBuffer(5000);

                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = null;
                        if(Build.VERSION.SDK_INT < 21)
                            dstBuf = codecInputBuffers[inputBufIndex];
                        else dstBuf = codec.getInputBuffer(inputBufIndex);
                        dstBuf.clear();

                        int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            totalBytesRead += bytesRead;
                            dstBuf.put(tempBuffer, 0, bytesRead);
                            codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                            presentationTimeUs = 1000000l * (totalBytesRead / 4) / 44100;
                        }
                    }
                }
                int outputBufIndex = 0;
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if(playlistFragment.isFinish()) break;
                    outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, 5000);
                    if (outputBufIndex >= 0) {
                        ByteBuffer encodedData = null;
                        if(Build.VERSION.SDK_INT < 21)
                            encodedData = codecOutputBuffers[outputBufIndex];
                        else encodedData = codec.getOutputBuffer(outputBufIndex);
                        encodedData.position(outBuffInfo.offset);
                        encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                        if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }else{
                            int outBitsSize   = outBuffInfo.size;
                            int outPacketSize = outBitsSize;
                            byte[] data = new byte[outPacketSize];
                            encodedData.get(data, 0, outBitsSize);
                            encodedData.position(outBuffInfo.offset);
                            encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                            mux.writeSampleData(audioTrackIdx, encodedData, outBuffInfo);
                            encodedData.clear();
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat = codec.getOutputFormat();
                        audioTrackIdx = mux.addTrack(outputFormat);
                        videoTrackIdx = mux.addTrack(videoFormat);
                        mux.start();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    } else {
                    }
                }
                int nComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0 / 4.0);
                publishProgress(50 + nComplete);
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            fis.close();

            int offset = 0;
            int sampleSize = 1024 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            long videoPresentationTimeUs = 0;
            long lastEndVideoTimeUs = 0;
            while (true) {
                if(playlistFragment.isFinish()) break;
                videoBufferInfo.offset = offset;
                int readVideoSampleSize = videoExtractor.readSampleData(videoBuf, offset);
                if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    videoBufferInfo.size = 0;
                } else if (readVideoSampleSize  < 0) {
                    videoExtractor.unselectTrack(0);
                    if (videoPresentationTimeUs >= presentationTimeUs) {
                        break;
                    }
                    else {
                        lastEndVideoTimeUs = videoPresentationTimeUs;
                        videoExtractor.selectTrack(0);
                        continue;
                    }
                } else {
                    long videoSampleTime = videoExtractor.getSampleTime();
                    videoBufferInfo.size = readVideoSampleSize;
                    videoBufferInfo.presentationTimeUs = videoSampleTime + lastEndVideoTimeUs;
                    if (videoBufferInfo.presentationTimeUs > presentationTimeUs) {
                        videoExtractor.unselectTrack(0);
                        break;
                    }
                    videoPresentationTimeUs = videoBufferInfo.presentationTimeUs;
                    videoBufferInfo.offset = 0;
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();

                    mux.writeSampleData(videoTrackIdx, videoBuf, videoBufferInfo);
                    videoExtractor.advance();
                }
                int nComplete = (int) Math.round(((float) videoPresentationTimeUs / (float) presentationTimeUs) * 100.0 / 4.0);
                publishProgress(75 + nComplete);
            }
            mux.stop();
            mux.release();
            mux = null;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if(codec != null) {
                codec.flush();
                codec.stop();
                codec.release();
            }
            if (inputFile.exists()) inputFile.delete();
            if (inputMp4File.exists()) inputMp4File.delete();
        }

        return 0;
    }

    @Override
    protected  void onProgressUpdate(Integer... progress)
    {
        playlistFragment.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(Integer result)
    {
        playlistFragment.finishSaveSongToGallery2(nLength, strMP4Path, alert, strPathTo);
    }
}