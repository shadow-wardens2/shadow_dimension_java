package Entities.Tutorials;

public class Lecon {
    private int id;
    private String titre;
    private String contenu;
    private int ordre;
    private Formation formation;
    private String image;
    private String videoUrl;
    private String documentUrl;
    private String videoDuration;
    private String videoThumbnail;

    public Lecon() {
    }

    public Lecon(int id, String titre, String contenu, int ordre, Formation formation, String image, String videoUrl,
            String documentUrl, String videoDuration, String videoThumbnail) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.ordre = ordre;
        this.formation = formation;
        this.image = image;
        this.videoUrl = videoUrl;
        this.documentUrl = documentUrl;
        this.videoDuration = videoDuration;
        this.videoThumbnail = videoThumbnail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(String videoDuration) {
        this.videoDuration = videoDuration;
    }

    public String getVideoThumbnail() {
        return videoThumbnail;
    }

    public void setVideoThumbnail(String videoThumbnail) {
        this.videoThumbnail = videoThumbnail;
    }

    @Override
    public String toString() {
        return this.titre != null ? this.titre : "New Lecon";
    }
}
