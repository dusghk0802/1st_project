package project.dto;

public class Category {
    private long categoryId;
    private long boardId;
    private String categoryName;
    private int sortOrder;

    public Category() {}

    public Category(long boardId, String categoryName, int sortOrder) {
        this.boardId = boardId;
        this.categoryName = categoryName;
        this.sortOrder = sortOrder;
    }

    public Category(long categoryId, long boardId, String categoryName, int sortOrder) {
        this.categoryId = categoryId;
        this.boardId = boardId;
        this.categoryName = categoryName;
        this.sortOrder = sortOrder;
    }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public long getBoardId() { return boardId; }
    public void setBoardId(long boardId) { this.boardId = boardId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}