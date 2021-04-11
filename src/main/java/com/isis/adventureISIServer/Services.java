/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.isis.adventureISIServer;

import com.isis.adventureISIServer.generated.PallierType;
import com.isis.adventureISIServer.generated.PalliersType;
import com.isis.adventureISIServer.generated.ProductType;
import com.isis.adventureISIServer.generated.ProductsType;
import com.isis.adventureISIServer.generated.TyperatioType;
import com.isis.adventureISIServer.generated.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author dell
 */
public class Services {

    public World readWorldFromXml(String username) throws JAXBException {

        JAXBContext jaxbContext;
        World world = new World();
        jaxbContext = JAXBContext.newInstance(World.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        try {
            InputStream input = new FileInputStream(username + "-world.xml");
            world = (World) jaxbUnmarshaller.unmarshal(input);
        } catch (Exception e) {
            InputStream input1 = getClass().getClassLoader().getResourceAsStream("world.xml");
            world = (World) jaxbUnmarshaller.unmarshal(input1);
        }

        return world;
    }

    public void saveWorldXml(World world, String pseudo) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(World.class);
            Marshaller march = jaxbContext.createMarshaller();
            march.marshal(world, new FileOutputStream(pseudo + "-world.xml"));
        } catch (Exception ex) {
            System.out.println("Erreur écriture du fichier:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public World getWorld(String pseudo) throws JAXBException {
        World world = this.readWorldFromXml(pseudo);
        if (world.getLastupdate() == 0) {
            world.setLastupdate(System.currentTimeMillis());
            this.saveWorldXml(world, pseudo);
        }
        updateWorld(world);
        return world;
    }

    public ProductType findProductById(World world, int id) {
        List<ProductType> produits = world.getProducts().getProduct();

        for (ProductType p : produits) {
            if (p.getId() == id) {
                return p;
            }

        }
        return null;
    }

    public Boolean updateProduct(String username, ProductType newproduct) throws JAXBException {
        // aller chercher le monde qui correspond au joueur

        World world = getWorld(username);
        // trouver dans ce monde, le produit équivalent à celui passé
        // en paramètre

        ProductType product = findProductById(world, newproduct.getId());
        System.out.println(product);
        if (product == null) {
            return false;
        }
        // calculer la variation de quantité. Si elle est positive c'est
        // que le joueur a acheté une certaine quantité de ce produit
        // sinon c’est qu’il s’agit d’un lancement de production.
        int qtchange = newproduct.getQuantite() - product.getQuantite();
        if (qtchange > 0) {
            double argent = world.getMoney();
            double debit = (qtchange * product.getCout()) + (qtchange * product.getCroissance());
            
            world.setMoney(argent);
            product.setQuantite(newproduct.getQuantite());

        } else {
            product.setTimeleft(product.getVitesse());
            world.setLastupdate(System.currentTimeMillis());
        }
        List<PallierType> palliers = product.getPalliers().getPallier();
        for (PallierType m : palliers) {
            int quantite = product.getQuantite();
            int seuil = m.getSeuil();

            if (quantite >= seuil) {

                TyperatioType p = m.getTyperatio();
                TyperatioType g = TyperatioType.GAIN;
                TyperatioType v = TyperatioType.VITESSE;
                TyperatioType a = TyperatioType.ANGE;

                if (p.equals(g)) {
                    double multip = product.getRevenu() * m.getRatio();
                    product.setRevenu(multip);
                    m.setUnlocked(true);
                } else if (p.equals(v)) {
                    double nvVitesse = product.getVitesse() / m.getRatio();
                    product.setVitesse((int) nvVitesse);
                    m.setUnlocked(true);
                } else if (p.equals(a)) {
                    double newratio = world.getAngelbonus() + m.getRatio();
                    world.setAngelbonus((int) newratio);

                }
            }

        }
        // sauvegarder les changements du monde

        saveWorldXml(world, username);
        return true;
    }

    public Boolean updateManager(String username, PallierType newmanager) throws JAXBException {

        // aller chercher le monde qui correspond au joueur
        World world = getWorld(username);
        // trouver dans ce monde, le manager équivalent à celui passé
        // en paramètre
        PallierType manager = findManagerByName(world, newmanager.getName());
        System.out.println(manager);
        if (manager == null) {
            return false;
        }

        // débloquer ce manager
        // trouver le produit correspondant au manager
        ProductType product = findProductById(world, manager.getIdcible());
        if (product == null) {
            return false;
        }
        // débloquer le manager de ce produit

        manager.setUnlocked(true);
        int cout = manager.getSeuil();
        double newmoney = world.getMoney() - cout;
        world.setMoney(newmoney);
        saveWorldXml(world, username);
        return true;
    }

    public PallierType findManagerByName(World world, String name) {
        List<PallierType> managers = world.getManagers().getPallier();
        PallierType manager = new PallierType();
        System.out.println(manager);
        for (PallierType m : managers) {
            if (m.getName().equals(name)) {
                manager = m;
                System.out.println("Je suis le manager :" + manager);
                return manager;
            } else {
                manager = null;
            }

        }
        return null;
    }

    public Boolean updateWorld(World world) throws JAXBException {
        // aller chercher le monde qui correspond au joueur        
        Double revenu = 0.0;
        Long passe = System.currentTimeMillis() - world.getLastupdate();
        if (passe != 0) {
            List<ProductType> produits = world.getProducts().getProduct();

            for (ProductType p : produits) {
                if (p.getTimeleft() <= passe && p.getTimeleft() != 0) {
                    revenu = p.getRevenu() * p.getQuantite()*(1+world.getActiveangels()*world.getAngelbonus()/100);
                }
                int nbProduits = (int) (passe / p.getVitesse());

                if (p.isManagerUnlocked()) {
                    revenu = nbProduits * (p.getRevenu())*(1+world.getActiveangels()*world.getAngelbonus()/100);
                    long time = passe % p.getVitesse();
                    if (time > 0) {
                        p.setTimeleft(time);
                    }
                }

            }
            world.setMoney(world.getMoney() + revenu);
            world.setScore(world.getScore() + revenu);
            System.out.println("score ici:"+world.getScore() + revenu);
            world.setLastupdate(System.currentTimeMillis());
        }

        return true;
    }

    public Boolean updateUpgrade(String username, PallierType newupgrade) throws JAXBException {

        // aller chercher le monde qui correspond au joueur
        World world = getWorld(username);
        // trouver dans ce monde, le manager équivalent à celui passé
        // en paramètre
        PallierType upgrade = findUpgradeByName(world, newupgrade.getName());
        if (upgrade == null) {
            return false;
        }
        System.out.println(upgrade);
        upgrade.setUnlocked(true);
        int cout = upgrade.getSeuil();
        double newmoney = world.getMoney() - cout;

        world.setMoney(newmoney);
        TyperatioType p = upgrade.getTyperatio();
        TyperatioType g = TyperatioType.GAIN;
        TyperatioType v = TyperatioType.VITESSE;
        // débloquer ce manager
        if (upgrade.getIdcible() == 0) {
            List<ProductType> produits = world.getProducts().getProduct();
            for (ProductType prod : produits) {
                if (p.equals(g)) {
                    double multip = prod.getRevenu() * upgrade.getRatio();
                    prod.setRevenu(multip);
                } else if (p.equals(v)) {
                    double nvVitesse = prod.getVitesse() / upgrade.getRatio();
                    prod.setVitesse((int) nvVitesse);

                }
            }
        } else if (upgrade.getIdcible() == -1) {
            double newratio = world.getAngelbonus() + upgrade.getRatio();
            world.setAngelbonus((int) newratio);
        } else {
            // trouver le produit correspondant au manager
            ProductType product = findProductById(world, upgrade.getIdcible());
            if (product == null) {
                return false;
            }
            // débloquer le manager de ce produit

            if (p.equals(g)) {
                double multip = product.getRevenu() * upgrade.getRatio();
                product.setRevenu(multip);
            } else if (p.equals(v)) {
                double nvVitesse = product.getVitesse() / upgrade.getRatio();
                product.setVitesse((int) nvVitesse);
            }
        }
        updateWorld(world);
        saveWorldXml(world, username);
        return true;
    }

    public PallierType findUpgradeByName(World world, String name) {
        List<PallierType> upgrades = world.getUpgrades().getPallier();
        PallierType upgrade = new PallierType();
        for (PallierType u : upgrades) {
            if (u.getName().equals(name)) {
                upgrade = u;
            } else {
                u = null;
            }

        }
        return upgrade;
    }

    public World resetWorld(String pseudo) throws JAXBException {
        World world = this.getWorld(pseudo);
        Double score = world.getScore();
        System.out.println("score :" + score);
        //Double AngeSupp = 150 * Math.sqrt(score / Math.pow(10, 15)) - world.getTotalangels();
        Double AngeSupp = 15*score;
        Double active = world.getActiveangels()+AngeSupp;
        Double total = world.getTotalangels()+ AngeSupp;
        System.out.println("anges :" + AngeSupp);
        world.setActiveangels(active);
        world.setTotalangels(total);

        System.out.println("active :" + world.getActiveangels());
        System.out.println("total :" + world.getTotalangels());
        World world1 = new World();
        saveWorldXml(world1, pseudo);
        JAXBContext jaxbContext;
        jaxbContext = JAXBContext.newInstance(World.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        InputStream input1 = getClass().getClassLoader().getResourceAsStream("world.xml");
        World nvWorld = (World) jaxbUnmarshaller.unmarshal(input1);
       
        nvWorld.setScore(score);
        nvWorld.setTotalangels(total);
        nvWorld.setActiveangels(active);
        saveWorldXml(nvWorld, pseudo);
        return nvWorld;

    }

    public Boolean updateAngel(String username, PallierType newangel) throws JAXBException {

        // aller chercher le monde qui correspond au joueur
        World world = getWorld(username);
        // trouver dans ce monde, le manager équivalent à celui passé
        // en paramètre
        PallierType upgrade = findAngelByName(world, newangel.getName());
        if (upgrade == null) {
            return false;
        }
        System.out.println(upgrade);
        upgrade.setUnlocked(true);
        int cout = upgrade.getSeuil();
        double newAngelmoney = world.getActiveangels() - cout;

        world.setActiveangels(newAngelmoney);
        TyperatioType p = upgrade.getTyperatio();
        TyperatioType g = TyperatioType.GAIN;
        TyperatioType v = TyperatioType.VITESSE;
        // débloquer ce manager
        if (upgrade.getIdcible() == 0) {
            List<ProductType> produits = world.getProducts().getProduct();
            for (ProductType prod : produits) {
                if (p.equals(g)) {
                    double multip = prod.getRevenu() * upgrade.getRatio();
                    prod.setRevenu(multip);
                } else if (p.equals(v)) {
                    double nvVitesse = prod.getVitesse() / upgrade.getRatio();
                    prod.setVitesse((int) nvVitesse);

                }
            }
        } else if (upgrade.getIdcible() == -1) {
            double newratio = world.getAngelbonus() + upgrade.getRatio();
            world.setAngelbonus((int) newratio);

        } else {
            // trouver le produit correspondant au manager
            ProductType product = findProductById(world, upgrade.getIdcible());
            if (product == null) {
                return false;
            }
            // débloquer le manager de ce produit

            if (p.equals(g)) {
                double multip = product.getRevenu() * upgrade.getRatio();
                product.setRevenu(multip);
            } else if (p.equals(v)) {
                double nvVitesse = product.getVitesse() / upgrade.getRatio();
                product.setVitesse((int) nvVitesse);
            }
        }
        saveWorldXml(world, username);
        return true;
    }

    public PallierType findAngelByName(World world, String name) {
        List<PallierType> anges = world.getAngelupgrades().getPallier();
        PallierType ange = new PallierType();
        for (PallierType a : anges) {
            if (a.getName().equals(name)) {
                ange = a;
            } else {
                a = null;
            }

        }
        return ange;
    }

}
