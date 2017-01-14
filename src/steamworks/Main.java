package steamworks;

import java.util.HashMap;
import java.util.Random;
import java.lang.Math;

public class Main {
    static class GearbotSimulation {
        static final boolean SHOW_TASKS = false;
        static final boolean SHOW_FINAL = false;

        Random rng = new Random();
        int rotor_in_progress = 0;
        int gears_needed[] = { 1, 2, 4, 6 };
        boolean auton = true;
        double time_remaining = 0;
        int score = 0;

        public int play() {
            try {
                auton = true;
                time_remaining = 15;

                drive_from_alliance_wall_to_peg();
                if (chance_of_not_dropping_gear()) {
                    place_gear_on_peg();
                    lift_gear();
                }

                if (time_remaining > 0) {
                    // if done early, wait until auton ends
                    time_remaining = 0;
                }
            }
            catch (TimeExpires e) {
                // auton ends
            }

            try {
                auton = false;
                time_remaining += 2 * 60 + 15;

                pilot_places_reserve_gear();

                // try to do another gear cycle or start climbing?
                while (time_remaining > 30) {
                    drive_from_peg_to_midfield();
                    if (chance_of_being_defended()) {
                        use_time(sample(4, 1), "blocked");
                    }
                    drive_from_midfield_to_alliance_feeder();
                    load_gear_from_feeder();
                    drive_from_alliance_feeder_to_midfield();
                    if (chance_of_being_defended()) {
                        use_time(sample(6, 1), "blocked");
                    }
                    drive_from_midfield_to_peg();
                    if (chance_of_not_dropping_gear()) {
                        place_gear_on_peg();
                        lift_gear();
                    }
                }

                drive_from_peg_to_rope();
                climb_rope();
            }
            catch (TimeExpires e) {
                // game ends
            }

            if (SHOW_FINAL) {
                System.out.println("*** FINAL " + score + " TIME=" + time_remaining);
            }

            return score;
        }

        boolean chance_of_not_dropping_gear() {
            return random_event(0.90);
        }

        boolean chance_of_being_defended() {
            return random_event(0.0);
        }

        void drive_from_alliance_wall_to_peg() throws TimeExpires {
            use_time(sample(3, 0.1), "driving");
        }

        void place_gear_on_peg() throws TimeExpires {
            use_time(sample(1, 0.1), "place-gear");
        }

        void lift_gear() throws TimeExpires {
            double robot_time_needed = sample(1, 0.1);
            double human_time_needed = sample(2, 0.1);
            double total_time_needed = robot_time_needed + human_time_needed;

            gears_needed[rotor_in_progress] -= 1;

            if (gears_needed[rotor_in_progress] == 0) {
                rotor_in_progress += 1;

                if (time_remaining > total_time_needed) {
                    score += (auton) ? 60 : 40;
                }
                else if (auton) {
                    // if there's not enough time to place the gear
                    // it will be scored during teleop
                    score += 40;
                }
            }

            use_time(robot_time_needed, "lift-gear");
        }

        void pilot_places_reserve_gear() {
            gears_needed[rotor_in_progress] -= 1;

            if (gears_needed[rotor_in_progress] == 0) {
                rotor_in_progress += 1;
                score += 40;
            }

            if (SHOW_TASKS) {
                System.out.println("reserve-gear 0.0 " + time_remaining + " " + score);
            }
        }

        void drive_from_peg_to_midfield() throws TimeExpires {
            use_time(sample(3, 1, 0), "driving");
        }

        void drive_from_midfield_to_alliance_feeder() throws TimeExpires {
            use_time(sample(5, 2), "driving");
        }

        void load_gear_from_feeder() throws TimeExpires {
            use_time(sample(2, 0.5), "load-gear");
        }

        void drive_from_alliance_feeder_to_midfield() throws TimeExpires {
            use_time(sample(3, 1, 0), "driving");
        }

        void drive_from_midfield_to_peg() throws TimeExpires {
            use_time(sample(5, 2), "driving");
        }

        void drive_from_peg_to_rope() throws TimeExpires {
            use_time(sample(2, 0.5), "driving");
        }

        void climb_rope() throws TimeExpires {
            double robot_time_needed = sample(6, 1);

            if (time_remaining > robot_time_needed) {
                score += 50;
            }

            use_time(robot_time_needed, "climbing");
        }

        void check_time() throws TimeExpires {
            if (time_remaining <= 0) {
                throw new TimeExpires();
            }
        }

        void use_time(double needed, String task) throws TimeExpires {
            if (SHOW_TASKS) {
                System.out.println(task + " " + needed + " " + time_remaining + " " + score);
            }
            time_remaining -= needed;
            check_time();
        }

        boolean random_event(double probability) {
            return rng.nextDouble() <= probability;
        }

        double sample(double mean, double std_dev) {
            double range = 3 * std_dev;
            return sample(mean, std_dev, -range, range);
        }

        double sample(double mean, double std_dev, double min) {
            double range = 3 * std_dev;
            return sample(mean, std_dev, min, range);
        }

        double sample(double mean, double std_dev, double min, double max) {
            double offset = 0;
            if (close_to(min, 0)) {
                do {
                    offset = rng.nextGaussian() * std_dev;
                    if (offset < 0) {
                        offset = -offset;
                    }
                } while (max < offset);
            }
            else {
                do {
                    offset = rng.nextGaussian() * std_dev;
                } while (offset < min || max < offset);
            }
            return mean + offset;
        }

        boolean close_to(double x, double y) {
            return close_to(x, y, 0.0001);
        }

        boolean close_to(double x, double y, double delta) {
            return Math.abs(x - y) < delta;
        }

        static class TimeExpires extends Exception {
        }
    }

    public static void main(String[] args) {
        HashMap<Integer, Integer> results = new HashMap<>();
        final int samples = 1000;
        for (int i = 0; i < samples; ++i) {
            GearbotSimulation match = new GearbotSimulation();
            int score = match.play();
            if (results.containsKey(score)) {
                results.put(score, results.get(score) + 1);
            }
            else {
                results.put(score, 1);
            }
        }
        System.out.println("score=occurrences out of " + samples + " " + results);
    }
}
