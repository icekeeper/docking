package ru.ifmo.docking.calculations.dockers;

//public class ElDocker extends GeometryDocker {
//
//    final double minElDelta;
//    final double maxElDelta;
//
//    public ElDocker(File complexDir, String complex) {
//        super(complexDir, complex, config);
//        Pair<Double, Double> elDelta = getElDelta();
//        this.minElDelta = -Math.log(elDelta.first);
//        this.maxElDelta = -Math.log(elDelta.second);
//    }
//
//    @Override
//    protected List<PointMatch> findTopCorrelatedPairs(int count) {
//        List<SpinImage> firstStack = computeSpinImageStack(receptorSurface);
//        System.out.println((System.currentTimeMillis() - start) + " First stack computed");
//        List<SpinImage> secondStack = computeSpinImageStack(ligandSurface);
//        System.out.println((System.currentTimeMillis() - start) + " Second stack computed");
//
//        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());
//
//        List<PointMatch> result = partition.parallelStream()
//                .flatMap(part -> {
//                    Queue<PointMatch> queue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.correlation, o2.correlation));
//                    for (SpinImage firstImage : firstStack) {
//                        for (SpinImage secondImage : part) {
//                            double correlation = firstImage.correlation(secondImage) + getElCorrelation(firstImage, secondImage);
//                            if (queue.size() < count || queue.peek().correlation < correlation) {
//                                queue.add(new PointMatch(firstImage.getPointIndex(), secondImage.getPointIndex(), correlation));
//                            }
//                            if (queue.size() > count) {
//                                queue.poll();
//                            }
//                        }
//                    }
//                    return queue.stream();
//                })
//                .collect(Collectors.toList());
//
//        result.sort((o1, o2) -> -Double.compare(o1.correlation, o2.correlation));
//
//        return Lists.newArrayList(result.subList(0, count));
//    }
//
//    private double getElCorrelation(SpinImage first, SpinImage second) {
//        double delta = -Math.log(Math.abs(receptorSurface.electricity[first.getPointIndex()] + ligandSurface.electricity[second.getPointIndex()]));
//        return (delta - minElDelta) / (maxElDelta - minElDelta);
//    }
//
//    private Pair<Double, Double> getElDelta() {
//        double min = Double.MAX_VALUE;
//        double max = Double.MIN_VALUE;
//        for (double firstEl : receptorSurface.electricity) {
//            for (double secondEl : ligandSurface.electricity) {
//                double delta = Math.abs(firstEl + secondEl);
//                if (min > delta) {
//                    min = delta;
//                }
//                if (max < delta) {
//                    max = delta;
//                }
//            }
//        }
//        return Pair.of(min, max);
//    }
//}