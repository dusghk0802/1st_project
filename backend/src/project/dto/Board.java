package project.dto;

public class Board {
    private long boardId;
    private String boardName;
    private String boardDescription;
    private int sortOrder;

    public Board() {}

    public Board(String boardName, String boardDescription, int sortOrder) {
        this.boardName = boardName;
        this.boardDescription = boardDescription;
        this.sortOrder = sortOrder;
    }

    public Board(long boardId, String boardName, String boardDescription, int sortOrder) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.boardDescription = boardDescription;
        this.sortOrder = sortOrder;
    }

    public long getBoardId() { return boardId; }
    public void setBoardId(long boardId) { this.boardId = boardId; }
    public String getBoardName() { return boardName; }
    public void setBoardName(String boardName) { this.boardName = boardName; }
    public String getBoardDescription() { return boardDescription; }
    public void setBoardDescription(String boardDescription) { this.boardDescription = boardDescription; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}