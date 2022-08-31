package com.amnesica.feedsta.models.sidecar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Sidecar implements Serializable {

  private static final long serialVersionUID = 1L;

  private ArrayList<SidecarEntry> sidecarEntries;

  public Sidecar(ArrayList<SidecarEntry> sidecarEntries) {
    this.sidecarEntries = sidecarEntries;
  }

  public int getMaxHeightOfSidecarEntries() {
    if (sidecarEntries != null) {
      return Collections.max(sidecarEntries).getHeight();
    }
    return 0;
  }

  public ArrayList<SidecarEntry> getSidecarEntries() {
    return sidecarEntries;
  }

  public void setSidecarEntries(ArrayList<SidecarEntry> sidecarEntries) {
    this.sidecarEntries = sidecarEntries;
  }
}
