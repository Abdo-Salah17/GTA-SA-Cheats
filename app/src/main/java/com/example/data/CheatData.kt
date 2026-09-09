package com.example.data

enum class CheatCategory(val title: String) {
    WEAPONS("Weapons & Combat"),
    WANTED("Wanted & Missions"),
    STATS("Player Stats"),
    GANGS("Gang & Peds"),
    WORLD("World & Traffic"),
    VEHICLES("Vehicles & Spawns")
}

data class CheatCode(
    val code: String,
    val name: String,
    val category: CheatCategory,
    val description: String = ""
)

object CheatDatabase {
    val allCheats: List<CheatCode> = listOf(
        // Weapons & Combat
        CheatCode("BEFWKSBQ", "Weapons 1", CheatCategory.WEAPONS, "Brass Knuckles, Bat, 9mm, Shotgun, Micro MP5, AK-47, Rifle, Rocket Launcher, Molotov, Spray Can"),
        CheatCode("SHHIHJJG", "Weapons 2", CheatCategory.WEAPONS, "Knife, Desert Eagle, Sawed-off, TEC-9, M4, Sniper, Flamethrower, Grenades, Extinguisher"),
        CheatCode("GOIZSSX", "Weapons 3", CheatCategory.WEAPONS, "Chainsaw, Silenced 9mm, SPAS-12, MP5, M4, Sniper, Stinger, Remote Explosives"),
        CheatCode("BIEUHQY", "Weapons 4", CheatCategory.WEAPONS, "Minigun, Night Vision Goggles, Thermal Goggles, Parachute and special gear"),
        CheatCode("SDWBWHE", "Max Weapon Skill", CheatCategory.WEAPONS, "Sets hitman level for all firearms"),
        CheatCode("PJYNQCQ", "$250k, Full Health & Armor", CheatCategory.WEAPONS, "Instant maximum health, full body armor, and quarter-million cash bonus"),
        CheatCode("SLOTSFK", "Suicide", CheatCategory.WEAPONS, "Immediately causes CJ to commit suicide and respawn"),
        CheatCode("NECUMZ", "Infinite Ammo", CheatCategory.WEAPONS, "Never run out of ammunition, no weapon reloading"),
        CheatCode("GONPXWR", "Infinite Health", CheatCategory.WEAPONS, "Invulnerability from bullets, fire, and direct attacks (except falling/explosions)"),
        CheatCode("POOOJOX", "Infinite Oxygen", CheatCategory.WEAPONS, "Unlimited lung capacity underwater without drowning"),

        // Wanted Level & Missions
        CheatCode("NCBXXDX", "Raise Wanted Level", CheatCategory.WANTED, "Adds 2 stars to current police wanted level"),
        CheatCode("KDTZNHO", "Lower Wanted Level", CheatCategory.WANTED, "Clears all active police wanted stars to zero"),
        CheatCode("GWJZWC", "6-Star Wanted Level", CheatCategory.WANTED, "Instantly summons maximum police, SWAT, and military response"),
        CheatCode("BYKGOAB", "Lock Wanted Level", CheatCategory.WANTED, "Freezes wanted stars so police never pursue CJ"),
        CheatCode("BYIXZIY", "Skip Current Mission", CheatCategory.WANTED, "Instantly completes the current active story mission"),

        // Player Stats & Abilities
        CheatCode("KBTMUVH", "Max Stamina", CheatCategory.STATS, "Unlimited sprint duration without CJ tiring out"),
        CheatCode("AESHXWQI", "Max Fat", CheatCategory.STATS, "Maximizes body fat meter"),
        CheatCode("SGVDSQW", "Max Muscle", CheatCategory.STATS, "Maximizes physical strength and physique meter"),
        CheatCode("KVGYZQK", "Low Muscle", CheatCategory.STATS, "Sets muscle status to minimum"),
        CheatCode("MTGIISR", "Max Respect", CheatCategory.STATS, "Maxes out street respect to recruit max gang members"),
        CheatCode("APGZLQR", "Max Sex Appeal", CheatCategory.STATS, "Sets sex appeal to 100%"),
        CheatCode("AFJKBNRP", "Max Gambling Skill", CheatCategory.STATS, "Highest stakes allowed in Las Venturas casinos"),
        CheatCode("TDBKCEH", "Bunny Hop", CheatCategory.STATS, "Massive bicycle bunny hop height"),
        CheatCode("LRMYOJM", "Super Jump", CheatCategory.STATS, "CJ jumps 10x higher than normal"),

        // Gang & Pedestrians
        CheatCode("EAMLJNN", "Fast Gang Spawn", CheatCategory.GANGS, "Gangs spawn rapidly across neighborhoods"),
        CheatCode("JEZRPI", "Recruit Pedestrians with Pistols", CheatCategory.GANGS, "Enables recruiting ordinary people armed with 9mm"),
        CheatCode("AWIOMPH", "Recruit Pedestrians with AK47", CheatCategory.GANGS, "Recruits carry heavy assault rifles"),
        CheatCode("QAONHOH", "Recruit Pedestrians with Rocket Launchers", CheatCategory.GANGS, "Recruits wield RPG rocket launchers"),
        CheatCode("HAPOHXR", "Gang Wars Active", CheatCategory.GANGS, "Triggers turf wars everywhere across cities"),
        CheatCode("HDLIWGB", "Carnival Mode", CheatCategory.GANGS, "Clown vehicles and dressed pedestrians"),
        CheatCode("BWCMMTD", "Beach Party Mode", CheatCategory.GANGS, "Swimwear crowd and beach buggies"),
        CheatCode("GDNXHDK", "Armed Pedestrians", CheatCategory.GANGS, "Every pedestrian carries random firearms"),
        CheatCode("FARYJHZ", "Riot Pedestrians", CheatCategory.GANGS, "City-wide riots with aggressive citizens"),
        CheatCode("NJXPCYE", "Chaos Mode", CheatCategory.GANGS, "Pedestrians riot and buildings burn"),
        CheatCode("AKOZBCH", "Golf Club Riot", CheatCategory.GANGS, "Pedestrians attack each other with golf clubs"),
        CheatCode("ERFBFNI", "Pedestrians Attack CJ", CheatCategory.GANGS, "Everyone in sight aggressively attacks CJ"),
        CheatCode("BNIZWSB", "Pimp Theme", CheatCategory.GANGS, "Pimp suits and adult pedestrian attire"),
        CheatCode("LNHVOAE", "Elvis Theme", CheatCategory.GANGS, "Elvis impersonators wander the streets"),
        CheatCode("AAUSQP", "Triad Theme", CheatCategory.GANGS, "Triad mobsters spawn with katanas"),
        CheatCode("OAXCCRI", "Rural Theme", CheatCategory.GANGS, "Country folk and rural farm vehicles populate cities"),

        // Traffic & World Environment
        CheatCode("DEHDRX", "Reduced Traffic", CheatCategory.WORLD, "Drastically decreases traffic volume on streets"),
        CheatCode("JTBCSN", "Rural Traffic", CheatCategory.WORLD, "Country tractors, trucks, and farm cars on roads"),
        CheatCode("FRIUBIL", "Sports Car Traffic", CheatCategory.WORLD, "Only exotic fast sports cars roam highways"),
        CheatCode("OWAKIJ", "Speed Up Time", CheatCategory.WORLD, "In-game game clock advances at high velocity"),
        CheatCode("EHWBWDS", "Fast Gameplay", CheatCategory.WORLD, "Game speed doubled for hyper fast movement"),
        CheatCode("FNJFCZC", "Slow Motion", CheatCategory.WORLD, "Cinematic bullet-time slow action"),
        CheatCode("SLSNRKKK", "Adrenaline Mode", CheatCategory.WORLD, "Slow-mo with superhuman punching strength"),
        CheatCode("ENQCFMA", "All Green Lights", CheatCategory.WORLD, "Every traffic signal stays permanently green"),
        CheatCode("IOKXTFJ", "Aggressive Traffic", CheatCategory.WORLD, "Drivers ram each other and CJ recklessly"),
        CheatCode("KTGDLXY", "Always 9 PM", CheatCategory.WORLD, "Freezes in-game time at dusk 21:00"),
        CheatCode("YACKMWS", "Skip 4 Hours", CheatCategory.WORLD, "Advances world clock by four hours forward"),
        CheatCode("AWUJNBB", "Always Midnight", CheatCategory.WORLD, "Locks time at 00:00 permanent dark night"),
        CheatCode("AAEXPPQC", "Sunny", CheatCategory.WORLD, "Clear bright sunny blue skies"),
        CheatCode("HTRTTVJ", "Very Hot", CheatCategory.WORLD, "Blistering desert heatwave weather"),
        CheatCode("VBWEMQX", "Cloudy", CheatCategory.WORLD, "Overcast cloudy sky with dim light"),
        CheatCode("TAVPIER", "Rainy", CheatCategory.WORLD, "Downpour rain storm with puddles"),
        CheatCode("EAKILHM", "Foggy", CheatCategory.WORLD, "Dense mist rolls across San Fierro and countryside"),
        CheatCode("EGCEBVM", "Sandstorm", CheatCategory.WORLD, "Violent desert sandstorm with low visibility"),
        CheatCode("JBWDWWO", "Foggy Weather", CheatCategory.WORLD, "Extra thick low-lying atmospheric fog"),

        // Vehicles & Spawning
        CheatCode("DLNNHZJ", "Perfect Handling", CheatCategory.WORLD, "Sharp turning grip and jumping cars on horn"),
        CheatCode("VQIMAHA", "Max Driving Skills", CheatCategory.VEHICLES, "Maximum driving, cycling, and flying proficiency"),
        CheatCode("BXBTUBTI", "Invincible Car", CheatCategory.VEHICLES, "Your vehicle is immune to crashes and bullets"),
        CheatCode("RYSMRM", "Driveby Aiming", CheatCategory.VEHICLES, "Full 360 manual aiming during vehicle drivebys"),
        CheatCode("DOTBSFK", "Flying Cars", CheatCategory.VEHICLES, "Automobiles take flight like aircraft"),
        CheatCode("PTHSEO", "Flying Boats", CheatCategory.VEHICLES, "Watercraft lift off water and fly through air"),
        CheatCode("WUSDOTO", "NOS All Cars", CheatCategory.VEHICLES, "Equips nitrous boost on all traffic vehicles"),
        CheatCode("VKYPQCF", "NOS Taxis", CheatCategory.VEHICLES, "All city taxis possess nitrous oxide boost"),
        CheatCode("GKPNMQ", "Drive on Water", CheatCategory.VEHICLES, "Cars glide across oceans and lakes safely"),
        CheatCode("BKFONFE", "Destroy All Cars", CheatCategory.VEHICLES, "Detonates all vehicles within visible range"),
        CheatCode("SDWBWHE", "Invisible Cars", CheatCategory.VEHICLES, "Only wheels remain visible on vehicles"),
        CheatCode("JBVIJXA", "Floating Cars", CheatCategory.VEHICLES, "Vehicles float away when hit by CJ"),
        CheatCode("GYKVYTR", "Pink Cars", CheatCategory.VEHICLES, "Paints entire city traffic pink"),
        CheatCode("GOYDVAO", "Black Cars", CheatCategory.VEHICLES, "Paints entire city traffic midnight black"),
        CheatCode("AYNVQVK", "Rhino Tank", CheatCategory.VEHICLES, "Spawns heavy military 60-ton Rhino Tank"),
        CheatCode("CDGUDEP", "Jetpack", CheatCategory.VEHICLES, "Spawns wearable military rocket propulsion jetpack"),
        CheatCode("AGBDLCID", "Monster Truck", CheatCategory.VEHICLES, "Spawns colossal 4x4 Monster Truck"),
        CheatCode("GSUMLEG", "Parachute", CheatCategory.VEHICLES, "Equips skydiving emergency parachute"),
        CheatCode("BIGLWCDD", "Bloodring Banger", CheatCategory.VEHICLES, "Spawns demolition derby demolition stock car"),
        CheatCode("DAHESZY", "Caddy", CheatCategory.VEHICLES, "Spawns golf course mini cart"),
        CheatCode("EEGCYXT", "Dozer", CheatCategory.VEHICLES, "Spawns heavy industrial bulldozer"),
        CheatCode("BGJPSYC", "Hotring Racer 1", CheatCategory.VEHICLES, "Spawns NASCAR style Hotring speedster A"),
        CheatCode("BIEAVBAY", "Hotring Racer 2", CheatCategory.VEHICLES, "Spawns NASCAR style Hotring speedster B"),
        CheatCode("AWPTMIIQ", "Hydra", CheatCategory.VEHICLES, "Spawns supersonic military VTOL jump fighter jet"),
        CheatCode("IXSMWCQ", "Limo", CheatCategory.VEHICLES, "Spawns luxury stretch limousine"),
        CheatCode("PSPNATX", "Romero", CheatCategory.VEHICLES, "Spawns Romero mortuary hearse"),
        CheatCode("HPGPIJZ", "Quad Bike", CheatCategory.VEHICLES, "Spawns all-terrain 4-wheel offroad quad"),
        CheatCode("URKQSRK", "Stunt Plane", CheatCategory.VEHICLES, "Spawns aerobatic red biplane"),
        CheatCode("AMOMHRER", "Tanker", CheatCategory.VEHICLES, "Spawns massive 18-wheel fuel tanker truck"),
        CheatCode("QPOLSVK", "Trashmaster", CheatCategory.VEHICLES, "Spawns heavy municipal garbage truck"),
        CheatCode("KGGGDKP", "Vortex", CheatCategory.VEHICLES, "Spawns amphibious terrain hovercraft")
    )

    // Quick lookup HashMap by code string
    val cheatHashMap: HashMap<String, CheatCode> = HashMap<String, CheatCode>().apply {
        for (cheat in allCheats) {
            put(cheat.code, cheat)
        }
    }

    // Categorized map for tabs and lists
    val categorizedCheats: Map<CheatCategory, List<CheatCode>> =
        CheatCategory.values().associateWith { category ->
            allCheats.filter { it.category == category }
        }
}
