package com.trekmate.config;

import com.trekmate.model.*;
import com.trekmate.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TrekRepository trekRepository;
    private final TrekDateRepository trekDateRepository;
    private final ActivityRepository activityRepository;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           TrekRepository trekRepository,
                           TrekDateRepository trekDateRepository,
                           ActivityRepository activityRepository) {
        this.userRepository = userRepository;
        this.trekRepository = trekRepository;
        this.trekDateRepository = trekDateRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedActivitiesAndFacilities();
        seedTreksAndDates();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            // Standard Trekker user
            User trekker = new User("Aryan Sharma", "trekker@example.com", "pass123", "+91 9876543210", "USER", new BigDecimal("45000.00"));
            userRepository.save(trekker);

            // Host / Expedition Leader
            User host = new User("Captain Vikram (Expedition Leader)", "host@example.com", "admin123", "+91 9123456789", "HOST", BigDecimal.ZERO);
            userRepository.save(host);

            System.out.println(">>> Demo users initialized:");
            System.out.println("    Trekker: trekker@example.com / pass123 (Credits: Rs 45,000)");
            System.out.println("    Host:    host@example.com / admin123");
        }
    }

    private void seedActivitiesAndFacilities() {
        if (activityRepository.count() == 0) {
            // Add-on Activities
            activityRepository.save(new Activity("Bungee Jumping", "ACTIVITY",
                    "Adrenaline-pumping 83m jump from India's highest cantilever platform.",
                    new BigDecimal("3500.00"), "PER_PERSON", "fa-bolt"));

            activityRepository.save(new Activity("White Water River Rafting", "ACTIVITY",
                    "Grade III-IV river rapid rapids with safety kayak escorts and wetsuits.",
                    new BigDecimal("1800.00"), "PER_PERSON", "fa-water"));

            activityRepository.save(new Activity("Tandem Paragliding", "ACTIVITY",
                    "Fly with a licensed pilot over majestic Himalayan alpine meadows.",
                    new BigDecimal("2800.00"), "PER_PERSON", "fa-wind"));

            activityRepository.save(new Activity("Cliff Zip-Lining", "ACTIVITY",
                    "High-speed aerial cable ride across deep mountain gorges.",
                    new BigDecimal("1200.00"), "PER_PERSON", "fa-mountain"));

            activityRepository.save(new Activity("Rock Climbing & Rappelling", "ACTIVITY",
                    "Learn technical ascending and descending on natural granite rock faces.",
                    new BigDecimal("1500.00"), "PER_PERSON", "fa-hiking"));

            activityRepository.save(new Activity("Campfire & Acoustic Night", "ACTIVITY",
                    "Starry night acoustic music, barbecue snacks, and constellation storytelling.",
                    new BigDecimal("850.00"), "PER_PERSON", "fa-fire"));

            // Add-on Facilities
            activityRepository.save(new Activity("Emergency Transport & Heli-Evac Insurance", "FACILITY",
                    "Comprehensive altitude sickness evacuation guarantee with ambulance standby.",
                    new BigDecimal("1200.00"), "PER_PERSON", "fa-ambulance"));

            activityRepository.save(new Activity("Certified Wilderness First-Aid Guide", "FACILITY",
                    "Dedicated mountaineering certified lead guide carrying pulse-oximeter and oxygen cylinder.",
                    new BigDecimal("3000.00"), "PER_GROUP", "fa-user-shield"));

            activityRepository.save(new Activity("Trekker High-Nutrition Food Plan", "FACILITY",
                    "4 hot meals daily: soup, carb-rich energy meals, trail nut mixes, and herbal tea.",
                    new BigDecimal("1600.00"), "PER_PERSON", "fa-utensils"));

            activityRepository.save(new Activity("Hygiene, Sanitization & Bio-Toilet Kit", "FACILITY",
                    "Eco-friendly wet wipes, sanitizers, dry wash shampoo, and portable bio-toilet tent access.",
                    new BigDecimal("600.00"), "PER_PERSON", "fa-pump-soap"));

            activityRepository.save(new Activity("Porter / Mule Baggage Service", "FACILITY",
                    "Dedicated porter or mule support to carry up to 10kg rucksack per group.",
                    new BigDecimal("2200.00"), "PER_GROUP", "fa-horse"));

            activityRepository.save(new Activity("High-Altitude Cold Gear Kit Rental", "FACILITY",
                    "-10C sleeping bag liner, microspikes/crampons, gaiters, and carbon trekking poles.",
                    new BigDecimal("950.00"), "PER_PERSON", "fa-mitten"));
        }
    }

    private void seedTreksAndDates() {
        if (trekRepository.count() == 0) {
            Trek t1 = new Trek(
                    "Kedarkantha Winter Snow Summit",
                    "Sankri, Uttarkashi",
                    "Uttarakhand",
                    "Easy",
                    6, 5,
                    new BigDecimal("9200.00"),
                    "One of India's most popular winter treks with 360-degree Himalayan panoramic views from 12,500 ft.",
                    3810,
                    "Beginner",
                    "December to April",
                    "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800"
            );

            Trek t2 = new Trek(
                    "Valley of Flowers & Hemkund Sahib",
                    "Govindghat, Chamoli",
                    "Uttarakhand",
                    "Moderate",
                    6, 5,
                    new BigDecimal("10800.00"),
                    "UNESCO World Heritage alpine biosphere sanctuary carpeted with 500+ species of wild exotic blossoms.",
                    4329,
                    "Average",
                    "July to September",
                    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800"
            );

            Trek t3 = new Trek(
                    "Hampta Pass & Chandratal Crescent Lake",
                    "Manali to Spiti Valley",
                    "Himachal Pradesh",
                    "Moderate",
                    5, 4,
                    new BigDecimal("11500.00"),
                    "Dramatic crossover trek from lush green valleys of Kullu into the barren desert landscape of Spiti.",
                    4287,
                    "Average",
                    "June to October",
                    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"
            );

            Trek t4 = new Trek(
                    "Triund Panoramic Ridge Trek",
                    "Dharamshala, McLeod Ganj",
                    "Himachal Pradesh",
                    "Easy",
                    2, 1,
                    new BigDecimal("3500.00"),
                    "The jewel of Dharamshala perched underneath the mighty snow-capped Dhauladhar mountain range.",
                    2828,
                    "Beginner",
                    "All Year Round",
                    "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800"
            );

            Trek t5 = new Trek(
                    "Roopkund Skeleton Lake Expedition",
                    "Lohajung, Garhwal",
                    "Uttarakhand",
                    "Challenging",
                    8, 7,
                    new BigDecimal("14500.00"),
                    "Enigmatic high-altitude glacial lake nestled in the lap of Trishul massif and lush Bedni Bugyal meadows.",
                    5029,
                    "High",
                    "May to June, Sept to Oct",
                    "https://images.unsplash.com/photo-1486870591958-9b9d0d1dda99?w=800"
            );

            Trek t6 = new Trek(
                    "Chadar Frozen River Ice Trek",
                    "Leh, Zanskar Valley",
                    "Ladakh",
                    "Difficult",
                    9, 8,
                    new BigDecimal("24000.00"),
                    "The ultimate winter survival adventure walking on the sheet of ice formed over the frozen Zanskar river in sub-zero temperatures.",
                    3390,
                    "Expert",
                    "January to February",
                    "https://images.unsplash.com/photo-1483921020237-2ff51e8e4b22?w=800"
            );

            Trek t7 = new Trek(
                    "Har Ki Dun Cradle of Shiva",
                    "Mori, Garhwal",
                    "Uttarakhand",
                    "Moderate",
                    7, 6,
                    new BigDecimal("12800.00"),
                    "Ancient cradle-shaped valley steeped in mythology with views of Swargarohini peak and 3000-year-old wooden villages.",
                    3566,
                    "Average",
                    "April to June, Sept to Dec",
                    "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=800"
            );

            Trek t8 = new Trek(
                    "Kudremukh Peak Rainforest Trail",
                    "Chikkamagaluru",
                    "Karnataka",
                    "Moderate",
                    2, 1,
                    new BigDecimal("4200.00"),
                    "Rolling shola grasslands and mist-cloaked evergreen tropical rainforest trail shaped like a horse face.",
                    1894,
                    "Average",
                    "October to February",
                    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"
            );

            List<Trek> treks = List.of(t1, t2, t3, t4, t5, t6, t7, t8);
            for (Trek t : treks) {
                Trek savedTrek = trekRepository.save(t);
                // Create upcoming batch dates (e.g. Next weekend, 2 weeks, 1 month)
                LocalDate now = LocalDate.now();
                trekDateRepository.save(new TrekDate(savedTrek, now.plusDays(7), now.plusDays(7 + savedTrek.getDurationDays()), 20));
                trekDateRepository.save(new TrekDate(savedTrek, now.plusDays(21), now.plusDays(21 + savedTrek.getDurationDays()), 15));
                trekDateRepository.save(new TrekDate(savedTrek, now.plusDays(35), now.plusDays(35 + savedTrek.getDurationDays()), 25));
            }
        }
    }
}

