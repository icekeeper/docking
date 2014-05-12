import org.junit.Test;
import ru.ifmo.docking.calculations.LipophilicityCalculator;
import ru.ifmo.docking.geometry.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LipophilicityCalculatorTest {

    @Test
    public void test1HPT() throws Exception {
        LipophilicityCalculator calculator = LipophilicityCalculator.construct(new File("data/1HPT_extracted_data/1HPT_extracted.pdb"), new File("data/fi_potentials.txt"));
        try (BufferedReader reader = new BufferedReader(new FileReader("data/1HPT_extracted_data/1HPT_extracted_lip.csv"))) {
            reader.lines().forEach(line -> {
                String[] tokens = line.split(",");
                double x = Double.parseDouble(tokens[0]);
                double y = Double.parseDouble(tokens[1]);
                double z = Double.parseDouble(tokens[2]);
                double fi = Double.parseDouble(tokens[3]);

                double newFi = calculator.compute(new Point(x, y, z));
                System.out.println(String.format("%e %e", fi, newFi));
            });
        }
    }
}
