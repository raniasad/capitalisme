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
import com.isis.adventureISIServer.generated.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
        try{
            InputStream input = new FileInputStream(username + "-world.xml");
             world = (World) jaxbUnmarshaller.unmarshal(input);
        }catch (Exception e){
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
        if (world.getLastupdate()==0){
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
            product.setQuantite(qtchange);

        } else {
            product.setTimeleft(product.getVitesse());
            world.setLastupdate(System.currentTimeMillis());
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
        product.getPalliers().getPallier().add(manager);
        int cout = manager.getSeuil();
        double newmoney = world.getMoney() - cout;
        world.setMoney(newmoney);

        saveWorldXml(world, username);
        return true;
    }

    public PallierType findManagerByName(World world, String name) {
        List<PallierType> managers = world.getManagers().getPallier();
        PallierType manager = new PallierType();
        for (PallierType m : managers) {
            if (m.getName().equals(name)) {
                manager = m;
            } else {
                manager = null;
            }

        }
        return manager;
    }

    public Boolean updateWorld(World world) throws JAXBException {
        // aller chercher le monde qui correspond au joueur        
        Double revenu = 0.0;
        Long passe = System.currentTimeMillis() - world.getLastupdate();
        if(passe!=0){
         List<ProductType> produits = world.getProducts().getProduct();
    
        for (ProductType p : produits) {
        if (p.getTimeleft() <= passe && p.getTimeleft()!=0) {
            revenu = p.getRevenu()*p.getQuantite();
        }
        int nbProduits = (int) (passe /p.getVitesse());
        
        if (p.isManagerUnlocked()) {
            revenu=nbProduits * (p.getRevenu());
            long time = passe % p.getVitesse();
            if (time > 0) {
                p.setTimeleft(time);
            }
        }
        
    }
        world.setMoney(world.getMoney()+revenu);
        world.setScore(world.getScore()+revenu);
        world.setLastupdate(System.currentTimeMillis());
        }
        
        return true;
    }

}
