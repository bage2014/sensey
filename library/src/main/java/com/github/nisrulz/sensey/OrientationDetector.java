/*
 * Copyright (C) 2016 Nishant Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.nisrulz.sensey;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;

public class OrientationDetector {

  private final int ORIENTATION_PORTRAIT = ExifInterface.ORIENTATION_ROTATE_90; // 6
  private final int ORIENTATION_LANDSCAPE_REVERSE = ExifInterface.ORIENTATION_ROTATE_180; // 3
  private final int ORIENTATION_LANDSCAPE = ExifInterface.ORIENTATION_NORMAL; // 1
  private final int ORIENTATION_PORTRAIT_REVERSE = ExifInterface.ORIENTATION_ROTATE_270; // 8
  private final int smoothness = 1;
  private final OrientationListener orientationListener;
  private final float[] pitches;
  private final float[] rolls;
  private float averagePitch = 0;
  private float averageRoll = 0;
  private int orientation = ORIENTATION_PORTRAIT;
  final SensorEventListener sensorEventListener = new SensorEventListener() {
    float[] mGravity;
    float[] mGeomagnetic;

    @Override public void onSensorChanged(SensorEvent event) {
      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mGravity = event.values;
      if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) mGeomagnetic = event.values;
      if (mGravity != null && mGeomagnetic != null) {
        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
        if (success) {
          float orientationData[] = new float[3];
          SensorManager.getOrientation(R, orientationData);
          averagePitch = addValue(orientationData[1], pitches);
          averageRoll = addValue(orientationData[2], rolls);
          orientation = calculateOrientation();
          switch (orientation) {
            case ORIENTATION_LANDSCAPE:
              orientationListener.onRightSideUp();
              break;
            case ORIENTATION_LANDSCAPE_REVERSE:
              orientationListener.onLeftSideUp();
              break;
            case ORIENTATION_PORTRAIT:
              orientationListener.onTopSideUp();
              break;
            case ORIENTATION_PORTRAIT_REVERSE:
              orientationListener.onBottomSideUp();
              break;
            default:
              // do nothing
              break;
          }
        }
      }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int i) {
      // do nothing
    }
  };

  public OrientationDetector(OrientationListener orientationListener) {
    this.orientationListener = orientationListener;

    pitches = new float[smoothness];
    rolls = new float[smoothness];
  }

  private float addValue(float value, float[] values) {
    float temp_value = (float) Math.round((Math.toDegrees(value)));
    float average = 0;
    for (int i = 1; i < smoothness; i++) {
      values[i - 1] = values[i];
      average += values[i];
    }
    values[smoothness - 1] = temp_value;
    average = (average + temp_value) / smoothness;
    return average;
  }

  private int calculateOrientation() {
    // finding local orientation dip
    if (((orientation == ORIENTATION_PORTRAIT || orientation == ORIENTATION_PORTRAIT_REVERSE) && (
        averageRoll > -30
            && averageRoll < 30))) {
      if (averagePitch > 0) {
        return ORIENTATION_PORTRAIT_REVERSE;
      } else {
        return ORIENTATION_PORTRAIT;
      }
    } else {
      // divides between all orientations
      if (Math.abs(averagePitch) >= 30) {
        if (averagePitch > 0) {
          return ORIENTATION_PORTRAIT_REVERSE;
        } else {
          return ORIENTATION_PORTRAIT;
        }
      } else {
        if (averageRoll > 0) {
          return ORIENTATION_LANDSCAPE_REVERSE;
        } else {
          return ORIENTATION_LANDSCAPE;
        }
      }
    }
  }

  public interface OrientationListener {
    void onTopSideUp();

    void onBottomSideUp();

    void onRightSideUp();

    void onLeftSideUp();
  }
}

