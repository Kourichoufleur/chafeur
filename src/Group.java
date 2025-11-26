public class Group {
    String nom_groupe;
    ArrayList<ClientRegistration> membres;

    public Group(String nom_groupe) {
        this.nom_groupe = nom_groupe;
        this.membres = new ArrayList<ClientRegistration>();
    }

    public void ajouter_membre(ClientRegistration client) {
        this.membres.add(client);
    }

    public void retirer_membre(ClientRegistration client) {
        this.membres.remove(client);
    }

    public ArrayList<ClientRegistration> get_membres() {
        return this.membres;
    }
}