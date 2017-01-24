package de.kdml.bigdatalab.spark_and_flink.flink.datacorn;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import de.kdml.bigdatalab.spark_and_flink.common_utils.StatisticsUtils;
import de.kdml.bigdatalab.spark_and_flink.common_utils.data.Trajectory;
import de.kdml.bigdatalab.spark_and_flink.flink.utils.FlinkUtils;

/**
 * Compute statistics per trajectory
 * 
 * @author Ehab Qadah
 * 
 *         Jan 2, 2017
 */
public class TrajectoriesStatistics {

	public static void main(String[] args) throws Exception {

		StreamExecutionEnvironment env = FlinkUtils.getInitializedEnv();

		/**
		 * Reduce the trajectories by aggregate the statics from the previous
		 * trajectory record and keep the new record, the further aggregation
		 * and discard old record.
		 * 
		 **/

		DataStream<Tuple2<String, Trajectory>> trajectoriesStream = TrajectoriesStreamUtils.getTrajectoriesStream(env)
				.reduce((Tuple2<String, Trajectory> tuple1, Tuple2<String, Trajectory> tuple2) -> {
					// compute & aggregate statistics for the new trajectory
					// based on the statistics of old trajectory
					// discard the old trajectory and just keep the new one
					Tuple2<String, Trajectory> oldTuple = tuple2.f1.isNew() ? tuple1 : tuple2;
					Tuple2<String, Trajectory> newTuple = tuple2.f1.isNew() ? tuple2 : tuple1;
					StatisticsUtils.computeStatistics(oldTuple.f1, newTuple.f1);
					newTuple.f1.setNew(false);
					return newTuple;
				});

		// showLatecies(trajectoriesStream);
		trajectoriesStream.print().setParallelism(1);

		env.execute(" Flink Trajectories Statistics Computation");
	}

	/**
	 * Show latencies of processed trajectories based on the time deference
	 * between streamed time and finished time
	 * 
	 * @param trajectoriesStream
	 */
	private static void showLatecies(DataStream<Tuple2<String, Trajectory>> trajectoriesStream) {
		DataStream<Long> latencies = trajectoriesStream.map(tuple -> {

			long currentTime = System.currentTimeMillis();
			// get last entered item
			Trajectory trajectory = tuple.f1;

			return new Long(currentTime - trajectory.getStreamedTime());

		});

		latencies.print().setParallelism(1);
	}
}
