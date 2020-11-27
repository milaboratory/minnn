package com.milaboratory.minnn.readfilter;

import static java.lang.Math.*;

public class Otsu {

    static int logOtsu(int[] data, int n_bins) {
        int[] hist = new int[n_bins];
        double[] bins = new double[n_bins + 1];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int sample : data) {
            if (sample < min) {
                min = sample;
            }
            if (sample > max) {
                max = sample;
            }
        }
        double logMin = log(min + 1);
        double dist = abs(log(max + 1) - logMin);
        double multi = n_bins / dist;
        for (int sample : data) {
            int index = (int) ((log(sample + 1) - logMin) * multi);
            if (index < hist.length) {
                hist[index]++;
            } else {
                hist[hist.length - 1]++;
            }
        }
        for (int i = 0; i < bins.length; i++) {
            bins[i] = logMin + dist * ((double) i / n_bins);
        }


        int samples_number = data.length;
        double mean_weigth = 1.0 / samples_number;
        int pcb = 0;
        int pcf = samples_number;
        double intence_pcb = 0;
        double intence_pcf = 0;
        double[] sum_arr = new double[hist.length];
        for (int i = 0; i < hist.length; i++) {
            sum_arr[i] = bins[i] * hist[i];
            intence_pcf += sum_arr[i];
        }
        int final_thresh = 0;
        double final_value = 0;
        for (int thresh = 0; thresh < hist.length; thresh++) {
            if (hist[thresh] > 0) {
                pcb += hist[thresh];
                pcf -= hist[thresh];
                if (pcf > 0) {
                    double Wb = pcb * mean_weigth;
                    double Wf = pcf * mean_weigth;
                    intence_pcb += sum_arr[thresh];
                    intence_pcf -= sum_arr[thresh];
                    double mub = intence_pcb / pcb;
                    double muf = intence_pcf / pcf;
                    double value = Wb * Wf * (mub - muf) * (mub - muf);
                    if (value > final_value) {
                        final_thresh = thresh + 1;
                        final_value = value;
                    }
                }
            }
        }
        return (int) (pow(E, bins[final_thresh]));
    }
}
