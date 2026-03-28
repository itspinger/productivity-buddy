package org.productivitybuddy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProcessInfo {
    private long pid;
    private double cpuUsage;
    private long ramUsageKb;

}
