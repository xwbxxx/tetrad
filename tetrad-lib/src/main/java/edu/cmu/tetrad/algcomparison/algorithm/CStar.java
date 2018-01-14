package edu.cmu.tetrad.algcomparison.algorithm;

import edu.cmu.tetrad.algcomparison.algorithm.oracle.pattern.Fges;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.Ida;
import edu.cmu.tetrad.util.ForkJoinPoolInstance;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.StatUtils;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import static java.lang.Math.abs;

@edu.cmu.tetrad.annotation.Algorithm(
        name = "CStar",
        command = "cstar",
        algoType = AlgType.forbid_latent_common_causes,
        description = "Performs a CStar analysis of the given dataset (Stekhoven, Daniel J., et al. " +
                "Causal stability ranking.\" Bioinformatics 28.21 (2012): 2819-2823) and returns a graph " +
                "in which all selected variables are shown as into the target. The target is the first variables."
)
public class CStar implements Algorithm {

    static final long serialVersionUID = 23L;
    private Algorithm algorithm;
    private Graph initialGraph = null;

    public CStar() {
        this.algorithm = new Fges();
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        DataSet _dataSet = (DataSet) dataSet;

        double percentageB = parameters.getDouble("percentSubsampleSize");
        int numSubsamples = parameters.getInt("numSubsamples");
        int q = parameters.getInt("topQRanks");

        Node y = dataSet.getVariables().get(0);

        List<Ida.NodeEffects> effects = new ArrayList<>();

        for (int i = 0; i < numSubsamples; i++) {
            BootstrapSampler sampler = new BootstrapSampler();
            sampler.setWithoutReplacements(true);
            DataSet sample = sampler.sample(_dataSet, (int) (percentageB * _dataSet.getNumRows()));

            Ida ida = new Ida(new CovarianceMatrixOnTheFly(sample));

            Ida.NodeEffects _effects = ida.getSortedMinEffects(y);

            effects.add(_effects);
        }

        final List<Node> variables = dataSet.getVariables();
        variables.remove(y);

        Map<Node, Integer> frequencies = new HashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            int f = 0;

            for (int j = 0; j < effects.size(); j++) {
                if (effects.get(j).getNodes().indexOf(variables.get(i)) < q) {
                    f++;
                }
            }

            frequencies.put(variables.get(i), f);
        }

        variables.sort((o1, o2) -> {
            final int d1 = frequencies.get(o1);
            final int d2 = frequencies.get(o2);
            return -Integer.compare(d1, d2);
        });

        double[] sortedFreqencies = new double[variables.size()];

        for (int i = 0; i < variables.size(); i++) {
            sortedFreqencies[i] = frequencies.get(variables.get(i));
        }

        System.out.println(Arrays.toString(sortedFreqencies));

        double[] ranks = StatUtils.getRanks(sortedFreqencies);


        int[] rankIndices = new int[variables.size()];
        
        int r = 1;
        double rr = ranks[0];

        for (int i = 0; i < ranks.length; i++) {
            if (ranks[i] == rr) {
                rankIndices[i] = r;
            } else {
                r++;
                rr = ranks[i];
                rankIndices[i] = r;
            }
        }

        System.out.println(Arrays.toString(ranks));
        System.out.println(Arrays.toString(rankIndices));

        Graph graph = new EdgeListGraph(dataSet.getVariables());

        for (int i = 0; i < variables.size(); i++) {
            if (rankIndices[i] <= q) {
                graph.addDirectedEdge(variables.get(i), y);
            }
        }

        return graph;
    }

    private void increment(Edge edge, Map<Edge, Integer> counts) {
        counts.putIfAbsent(edge, 0);
        counts.put(edge, counts.get(edge) + 1);
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return algorithm.getComparisonGraph(graph);
    }

    @Override
    public String getDescription() {
        return "CStar";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add("numSubsamples");
        parameters.add("percentSubsampleSize");
        parameters.add("topQRanks");

        return parameters;
    }
}
