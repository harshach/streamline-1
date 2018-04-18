package com.hortonworks.streamline.streams.actions.flink.topology;

import com.google.common.base.Joiner;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.layout.flink.FlinkTopologyLayoutConstants;
import org.apache.commons.io.IOUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlinkTopologyActionsImpl implements TopologyActions {

	private static final Logger LOG = LoggerFactory.getLogger(FlinkTopologyActionsImpl.class);
	private String flinkArtifactsLocation = "/tmp/flink-artifacts/";

	private String flinkBinaryPath ;

	public void init(Map<String, Object> conf) {
		if (conf.containsKey(FlinkTopologyLayoutConstants.FLINK_HOME_DIR)) {
			String flinkHomeDir = (String) conf.get(FlinkTopologyLayoutConstants.FLINK_HOME_DIR);
			if (!flinkHomeDir.endsWith(File.separator)) {
				flinkHomeDir += File.separator;
			}
			flinkBinaryPath = flinkHomeDir + "bin";
		}
		File f = new File (flinkArtifactsLocation);
		if (!f.exists() && !f.mkdirs()) {
			throw new RuntimeException("Could not create directory " + f.getAbsolutePath());
		}
	}

	private Process executeShellProcess (List<String> commands) throws Exception {
		LOG.debug("Executing command: {}", Joiner.on(" ").join(commands));
		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		processBuilder.redirectErrorStream(true);
		return processBuilder.start();
	}

	private static class ShellProcessResult {
		private final int exitValue;
		private final String stdout;
		ShellProcessResult(int exitValue, String stdout) {
			this.exitValue = exitValue;
			this.stdout = stdout;
		}
	}

	private ShellProcessResult waitProcessFor(Process process) throws IOException, InterruptedException {
		StringWriter sw = new StringWriter();
		IOUtils.copy(process.getInputStream(), sw, Charset.defaultCharset());
		String stdout = sw.toString();
		process.waitFor();
		int exitValue = process.exitValue();
		LOG.debug("Command output: {}", stdout);
		LOG.debug("Command exit status: {}", exitValue);
		return new ShellProcessResult(exitValue, stdout);
	}

	public void deploy(TopologyLayout topology, String mavenArtifacts, TopologyActionContext ctx, String asUser) throws Exception {
		LOG.info("XXXXXXXXXXXXXX deploy() XXXXXXXXXXXXXXXXXXX");
		// Start Flink cluster
		List<String> commands = new ArrayList<String>();
		commands.add(flinkBinaryPath + File.separator + "start-cluster.sh");
		LOG.info("Starting flink cluster {}", topology.getName());
		LOG.info(String.join(" ", commands));

		Process process = executeShellProcess(commands);
		ShellProcessResult shellProcessResult = waitProcessFor(process);
		int exitValue = shellProcessResult.exitValue;
		if (exitValue != 0) {
			LOG.error("Topology deploy command failed - exit code: {} / output: {}", exitValue, shellProcessResult.stdout);
			String[] lines = shellProcessResult.stdout.split("\\n");
			String errors = Arrays.stream(lines)
					.filter(line -> line.startsWith("Exception") || line.startsWith("Caused by"))
					.collect(Collectors.joining(", "));
			throw new Exception("Topology could not be deployed successfully: flink deploy command failed with " + errors);
		}

		// Build a JobGraph and submit it locally.
		FlinkJobGraphGenerator jobGraphGenerator = new FlinkJobGraphGenerator();
		topology.getTopologyDag().traverse(jobGraphGenerator);
		StreamExecutionEnvironment execEnv = StreamExecutionEnvironment.createLocalEnvironment();
		jobGraphGenerator.generateStreamGraph(execEnv);

		execEnv.execute("myTestJob1");
	}

	public void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts, Map<String, TestRunSource> testRunSourcesForEachSource, Map<String, TestRunProcessor> testRunProcessorsForEachProcessor, Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor, Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception {
		LOG.info("XXXXXXXXXXXXXX runTest() XXXXXXXXXXXXXXXXXXX");
	}

	public boolean killTest(TopologyTestRunHistory testRunHistory) {
		LOG.info("XXXXXXXXXXXXXX killTest() XXXXXXXXXXXXXXXXXXX");
		return false;
	}

	public void kill(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("XXXXXXXXXXXXXX kill() XXXXXXXXXXXXXXXXXXX");
	}

	public void validate(TopologyLayout topology) throws Exception {
		LOG.info("XXXXXXXXXXXXXX validate() XXXXXXXXXXXXXXXXXXX");
	}

	public void suspend(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("XXXXXXXXXXXXXX suspend() XXXXXXXXXXXXXXXXXXX");
	}

	public void resume(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("XXXXXXXXXXXXXX resume() XXXXXXXXXXXXXXXXXXX");
	}

	public Status status(TopologyLayout topology, String asUser) throws Exception {
		LOG.info("XXXXXXXXXXXXXX status() XXXXXXXXXXXXXXXXXXX");
		return null;
	}

	public LogLevelInformation configureLogLevel(TopologyLayout topology, LogLevel targetLogLevel, int durationSecs, String asUser) throws Exception {
		return null;
	}

	public LogLevelInformation getLogLevel(TopologyLayout topology, String asUser) throws Exception {
		return null;
	}

	private static String generateFlinkTopologyName(TopologyLayout topology) {
		return "streamline-" + topology.getId() + "-" + topology.getName();
	}

	public Path getArtifactsLocation(TopologyLayout topology) {
		LOG.info("XXXXXXXXXXXXXX getArtifactsLocation() XXXXXXXXXXXXXXXXXXX");
		return Paths.get(flinkArtifactsLocation, generateFlinkTopologyName(topology), "artifacts");
	}

	public Path getExtraJarsLocation(TopologyLayout topology) {
		LOG.info("XXXXXXXXXXXXXX getExtraJarsLocation() XXXXXXXXXXXXXXXXXXX");
		return Paths.get(flinkArtifactsLocation, generateFlinkTopologyName(topology), "jars");
	}

	public String getRuntimeTopologyId(TopologyLayout topology, String asUser) {
		LOG.info("XXXXXXXXXXXXXX getRuntimeTopologyId() XXXXXXXXXXXXXXXXXXX");
		throw new TopologyNotAliveException("Topology not found in Cluster - topology id: " + topology.getId());
	}
}
