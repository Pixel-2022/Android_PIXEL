package org.tensorflow.demo;

public class Data {
    private int id;
    private Boolean star;
    private int UserId;
    private String Word;
    private String image;

    public Data(int id, int userid, Boolean star, String Word, String image){
        this.id = id;
        this.UserId=userid;
        this.Word = Word;
        this.star = star;
        this.image=image;
    }
    public int getid(){
        return id;
    }
    public void setid(int id){
        this.id = id;
    }
    public int getUserId(){
        return UserId;
    }
    public void setUserId(int UserId){
        this.UserId = UserId;
    }
    public Boolean getStar(){
        return star;
    }
    public void setStar(Boolean star){
        this.star = star;
    }
    public String getWord(){
        return Word;
    }
    public void setWord(String Word){
        this.Word = Word;
    }
    public String getImage() {return image;}
    public void setImage(String image) {this.image = image;}
}

