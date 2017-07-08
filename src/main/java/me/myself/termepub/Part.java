package me.myself.termepub;

public class Part implements Comparable<Part> {

    private String title;
    private String originalLabel;
    private String content;
    private int index;
    private int realIndex;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalLabel() {
        return originalLabel;
    }

    public void setOriginalLabel(String originalLabel) {
        this.originalLabel = originalLabel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getRealIndex() {
        return realIndex;
    }

    public void setRealIndex(int realIndex) {
        this.realIndex = realIndex;
    }

    public Part(String title, String content, int index, int realIndex) {
        super();
        this.title = title;
        this.content = content;
        this.index = index;
        this.realIndex = realIndex;
    }

    public Part(String title, String originalLabel, String content, int index,
            int realIndex) {
        super();
        this.title = title;
        this.originalLabel = originalLabel;
        this.content = content;
        this.index = index;
        this.realIndex = realIndex;
    }

    public Part() {
    };

    public Part(String title, int index, int realIndex) {
        super();
        this.title = title;
        this.index = index;
        this.realIndex = realIndex;
    }

    @Override
    public int compareTo(Part other) {
        if (this.getContent().equals(other.getContent())) {
            return 0;
        }
        if (isNoLabelButSameTitle(this, other)
                || isNoLabelButSameTitle(other, this)) {
            return 0;
        }
        return this.getRealIndex() - other.getRealIndex();
    }

    private boolean isNoLabelButSameTitle(Part part1, Part part2) {
        return part1.getOriginalLabel() == null
                && (part2.getTitle().startsWith(part1.getTitle())
                        || part1.getTitle().startsWith(part2.getTitle()));

    }

    @Override
    public boolean equals(Object other) {
        if (this.getContent().equals(((Part) other).getContent())) {
            return true;
        }
        if (isNoLabelButSameTitle(this, (Part) other)
                || isNoLabelButSameTitle((Part) other, this)) {
            return true;
        }
        if (Math.abs(this.getIndex() - ((Part) other).getIndex()) == 1) {
            if (this.getTitle().startsWith(((Part) other).getTitle())
                    || ((Part) other).getTitle().startsWith(this.getTitle())) {
                return true;
            }
        }
        return super.equals(other);
    }

}
