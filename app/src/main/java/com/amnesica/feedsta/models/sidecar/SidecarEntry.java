package com.amnesica.feedsta.models.sidecar;

import java.io.Serializable;

import lombok.Data;

/** Represents a single sidecar entry */
@Data
public class SidecarEntry implements Serializable, Comparable<SidecarEntry> {

  private static final long serialVersionUID = 1L;

  private Enum<SidecarEntryType> sidecarEntryType;
  private int index;
  private String url;
  private int height;

  public SidecarEntry(Enum<SidecarEntryType> sidecarEntryType, int index, String url) {
    this.sidecarEntryType = sidecarEntryType;
    this.index = index;
    this.url = url;
  }

  @Override
  public int compareTo(SidecarEntry o) {
    if (this.getHeight() > o.getHeight()) {
      return 1;
    } else if (this.getHeight() < o.getHeight()) {
      return -1;
    }
    return 0;
  }
}
