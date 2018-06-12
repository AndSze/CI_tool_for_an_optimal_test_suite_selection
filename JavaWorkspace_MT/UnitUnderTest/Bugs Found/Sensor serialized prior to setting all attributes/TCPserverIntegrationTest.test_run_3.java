SensorImpl temp_sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, getMeasurements_limit());
processing_engine.saveSensorInfo(temp_sensor, "sensorINITIALIZATION");
temp_sensor.setSensor_watchdog_scale_factor(getWatchdogs_scale_factor());
Server_Sensors_LIST = processing_engine.updateServerSensorList(temp_sensor);

SensorImpl temp_sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, getMeasurements_limit());
temp_sensor.setSensor_watchdog_scale_factor(getWatchdogs_scale_factor());
processing_engine.saveSensorInfo(temp_sensor, "sensorINITIALIZATION");
Server_Sensors_LIST = processing_engine.updateServerSensorList(temp_sensor);