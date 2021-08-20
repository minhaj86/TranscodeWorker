package com.videostream.transcode;

public class TranscodeDTO {
  private String id;
  private String videoRef;
  private String targetFormat;
  private String targetAttribute;
  private int fileId;
  private String fileName;
  private String outputFileName;

  public TranscodeDTO(String id, String videoRef, String targetFormat, String targetAttribute) {
    this.id = id;
    this.videoRef = videoRef;
    this.targetFormat = targetFormat;
    this.targetAttribute = targetAttribute;
  }

  public TranscodeDTO() {
    this.id = "0000000";
    this.videoRef = "videoRef";
    this.targetFormat = "mp4";
    this.targetAttribute = "targetAttribute";
    this.fileId = 101;
    this.fileName = "ralph.mp4";
    this.outputFileName = "ralph_coded.mp4";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVideoRef() {
    return videoRef;
  }

  public void setVideoRef(String videoRef) {
    this.videoRef = videoRef;
  }

  public String getTargetFormat() {
    return targetFormat;
  }

  public void setTargetFormat(String targetFormat) {
    this.targetFormat = targetFormat;
  }

  public String getTargetAttribute() {
    return targetAttribute;
  }

  public void setTargetAttribute(String targetAttribute) {
    this.targetAttribute = targetAttribute;
  }

  public int getFileId() {
    return fileId;
  }

  public void setFileId(int fileId) {
    this.fileId = fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getOutputFileName() {
    return this.outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }
}
