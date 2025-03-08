import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// represents the whole game
class LightEmAll extends World {
  
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  
  // the width and height of the board
  int width;
  int height;
  
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  
  // how big a cell will be when drawn
  int cellsize;
  
  // keeps track of clicks
  int clicks;
  
  // keeps track of time in ticks
  int ticks;
  
  // keeps track of the best score (clicks * secs)
  int bestScore;
  
  
  // creates a new LightEmAll
  LightEmAll(int width, int height, int bias) {
    this.width = width;
    this.height = height;
    Random r = new Random();
    this.powerRow = r.nextInt(height);
    this.powerCol = r.nextInt(width);
    this.radius = Math.max(width / 2, height / 2);
    board = new ArrayList<ArrayList<GamePiece>>();
    this.boardInit(bias);
    this.cellsize = this.cellSizeInit();
    this.clicks = 0;
    this.ticks = 0;
    this.nodes = new ArrayList<GamePiece>();
    this.mst = new ArrayList<Edge>();
    
    // sets bestScore to a default of 1000
    this.bestScore = 1000;
  }
  
  // test constructor
  LightEmAll(ArrayList<ArrayList<GamePiece>> alg, int powerX, int powerY) {
    this(alg.get(0).size(), alg.size(), 0);
    this.board = alg;
    this.cellsize = 70;
    this.powerRow = powerY;
    this.powerCol = powerX;
    board.get(powerY).get(powerX).powerStation = true;
  }
  
  // scales cellsize
  public int cellSizeInit() {
    int wideCell = 750 / this.width;
    int highCell = 375 / this.height;
    if (wideCell >= highCell) {
      return highCell;
    }
    return wideCell;
  }
  
  // draws the board
  @Override
  public WorldScene makeScene() {
    WorldScene world = new WorldScene(cellsize * (width + 2), cellsize * (height + 3));
    
    // draws clicks button
    world.placeImageXY(new OverlayImage(new TextImage(Integer.toString(this.clicks), cellsize / 2,
        Color.black), new OverlayImage(new RectangleImage(cellsize, cellsize, OutlineMode.OUTLINE, 
        Color.black), new RectangleImage(cellsize, cellsize, OutlineMode.SOLID, Color.gray))), 
        cellsize * (width + 2) / 4, cellsize);
    
    // draws restart button
    if (endGame()) {
      world.placeImageXY(new OverlayImage(new TextImage("New Game", cellsize / 6,
          Color.black), new OverlayImage(new RectangleImage(cellsize, cellsize, 
              OutlineMode.OUTLINE, 
          Color.black), new RectangleImage(cellsize, cellsize, OutlineMode.SOLID, Color.gray))), 
          cellsize * (width + 2) / 2, cellsize);
    }
    else {
      world.placeImageXY(new OverlayImage(new TextImage("Restart", cellsize / 6,
          Color.black), new OverlayImage(new RectangleImage(cellsize, cellsize, 
              OutlineMode.OUTLINE, 
          Color.black), new RectangleImage(cellsize, cellsize, OutlineMode.SOLID, Color.gray))), 
          cellsize * (width + 2) / 2, cellsize);
    }
    
    // draws time button
    world.placeImageXY(new OverlayImage(new TextImage(Integer.toString((int)this.ticks), 
        cellsize / 2, Color.black), new OverlayImage(new RectangleImage(cellsize, cellsize, 
            OutlineMode.OUTLINE, Color.black), new RectangleImage(cellsize, cellsize, 
                OutlineMode.SOLID, Color.gray))), 3 * cellsize * (width + 2) / 4, cellsize);
    
    // draws each cell
    for (int i = 0; i < board.size(); i++) {
      for (int j = 0; j < board.get(i).size(); j++) {
        if (board.get(i).get(j).powered) {
          world.placeImageXY(board.get(i).get(j).tileImage(cellsize, 
              cellsize / 5, Color.yellow, false), cellsize * j + 3 * cellsize / 2, 
              cellsize * i + 5 * cellsize / 2);
        }
        else {
          world.placeImageXY(board.get(i).get(j).tileImage(cellsize, 
              cellsize / 5, Color.gray, false), cellsize * j + 3 * cellsize / 2, 
              cellsize * i + 5 * cellsize / 2);
        }
      }
    }
    // updates power every tick
    board.get(powerRow).get(powerCol).power(board);
    
    // if win
    if (this.endGame()) {
      if (this.clicks * this.ticks < this.bestScore) {
        this.bestScore = this.clicks * this.ticks;
      }
      world.placeImageXY(new TextImage("You win!", cellsize, Color.green), 
          cellsize * (width + 2) / 2, cellsize * (height + 2) / 3);
      world.placeImageXY(new TextImage("Score: " + Integer.toString(this.clicks * this.ticks), 
          2 * cellsize / 3, Color.green), cellsize * (width + 2) / 2, 
          cellsize * (height + 2) / 2);
      world.placeImageXY(new TextImage("Best Score: " + Integer.toString(this.bestScore), 
          cellsize / 2, Color.green), cellsize * (width + 2) / 2, 
          2 * cellsize * (height + 2) / 3);
    }
    
    return world;
    
  }
  
  // initializes the board
  public void boardInit(int bias) {
    // creates an empty board
    for (int i = 0; i < height; i++) {
      ArrayList<GamePiece> gparr = new ArrayList<GamePiece>();
      for (int j = 0; j < width; j++) {
        GamePiece gp = new GamePiece(false, false, false, false, j, i);
        gparr.add(gp);
      }
      board.add(gparr);
    }
    // sets a powerStation
    board.get(powerRow).get(powerCol).powerStation = true;
    // create MST
    this.mst = this.createMST(bias);
    // change board based on MST
    for (Edge e : mst) {
      GamePiece fromPiece = e.fromNode;
      GamePiece toPiece = e.toNode;
      // if fromPiece is to the left of toPiece
      if (fromPiece.col < toPiece.col) {
        fromPiece.right = true;
        toPiece.left = true;
      }
      // if fromPiece is to the right of toPiece
      else if (fromPiece.col > toPiece.col) {
        fromPiece.left = true;
        toPiece.right = true;
      }
      // if fromPiece is above toPiece
      else if (fromPiece.row < toPiece.row) {
        fromPiece.bottom = true;
        toPiece.top = true;
      }
      // if fromPiece is below toPiece
      else if (fromPiece.row > toPiece.row) {
        fromPiece.top = true;
        toPiece.bottom = true;
      }
    }
    // scramble the board
    for (ArrayList<GamePiece> agp : board) {
      for (GamePiece gp : agp) {
        // rotates each piece a random number of times between 0 and 3
        for (int i = 0; i < new Random().nextInt(4); i++) {
          gp.rotate();
        }
      }
    }
  }
  
  // detects where mouse was clicked
  public void onMouseClicked(Posn pos, String buttonName) {
    // reset functionality
    if (pos.y > cellsize / 2 && pos.y < 3 * cellsize / 2
        && pos.x > cellsize * width / 2 + 1 * cellsize / 2
        && pos.x < cellsize * width / 2 + 3 * cellsize / 2) {
      // resets everything
      this.clicks = 0;
      this.ticks = 0;
      this.nodes = new ArrayList<GamePiece>();
      this.mst = new ArrayList<Edge>();
      this.board = new ArrayList<ArrayList<GamePiece>>();
      this.powerRow = new Random().nextInt(height);
      this.powerCol = new Random().nextInt(width);
      // sets a random bias
      this.boardInit(new Random().nextInt(3));
    }
    // check to make sure not out of bounds
    if (!this.endGame() && (pos.x >= cellsize || pos.y >= 2 * cellsize)) {
      int x = (pos.x - cellsize) / cellsize;
      int y = (pos.y - 2 * cellsize) / cellsize;
      // checks to make sure a tile was clicked and if so clicks the tile
      if (x >= 0 && x < board.get(0).size() 
          && y >= 0 && y < board.size() && buttonName.equals("LeftButton")) {
        board.get(y).get(x).rotate();
      }
      clicks++;
    }
    // updates board to be not powered at all to acknowledge changes
    for (ArrayList<GamePiece> agp : board) {
      for (GamePiece gp : agp) {
        gp.powered = false;
      }
    }
    // updates power
    board.get(powerRow).get(powerCol).power(board);
  }
  
  // moves the power station
  public void onKeyEvent(String key) {
    if (!this.endGame()) {
      GamePiece powerPiece = board.get(powerRow).get(powerCol);
      int powRow = powerRow;
      int powCol = powerCol;
      if (key.equals("left") && powerCol - 1 >= 0 && powerPiece.left
          && board.get(powerRow).get(powerCol - 1).right) {
        powCol = powerCol - 1;
      }
      if (key.equals("right") && powerCol + 1 < width && powerPiece.right
          && board.get(powerRow).get(powerCol + 1).left) {
        powCol = powerCol + 1;
      }
      if (key.equals("up") && powerRow - 1 >= 0 && powerPiece.top
          && board.get(powerRow - 1).get(powerCol).bottom) {
        powRow = powerRow - 1;
      }
      if (key.equals("down") && powerRow + 1 < width && powerPiece.bottom
          && board.get(powerRow + 1).get(powerCol).top) {
        powRow = powerRow + 1;
      }
      powerPiece.powerStation = false;
      board.get(powRow).get(powCol).powerStation = true;
      this.powerRow = powRow;
      this.powerCol = powCol;
      clicks++;
    }
    // updates board to be not powered at all to acknowledge changes
    for (ArrayList<GamePiece> agp : board) {
      for (GamePiece gp : agp) {
        gp.powered = false;
      }
    }
    // updates power
    board.get(powerRow).get(powerCol).power(board);
  }
  
  // checks if the game should end
  public boolean endGame() {
    for (int i = 0; i < board.size(); i++) {
      for (int j = 0; j < board.get(i).size(); j++) {
        if (!board.get(i).get(j).powered) {
          return false;
        }
      }
    }
    return true;
  }
  
  // creates an MST to draw the board with a bias
  public ArrayList<Edge> createMST(int bias) {
    
    // if bias = 0, no bias
    // if bias = 1, horizontal bias
    // if bias = 2, vertical bias
    
    ArrayList<Edge> edges = new ArrayList<Edge>();
    ArrayList<CartPt> coords = new ArrayList<CartPt>();
    
    for (int col = 0; col < width; col = col + 1) {
      for (int row = 0; row < height; row = row + 1) {
        GamePiece currentPiece = board.get(col).get(row);
        int weight = new Random().nextInt(60);
        // if not top row adds a downwards edge with a random weight to edges
        if (row > 0) {
          // if bias is vertical
          if (bias == 2) {
            while (weight < 20) {
              weight = new Random().nextInt(60);
            }
          }
          
          edges.add(new Edge(currentPiece, board.get(col).get(row - 1), weight));
        }
        // if not bottom row adds a upwards edge with a random weight to edges
        if (row < height - 1) {
          // if bias is vertical
          if (bias == 2) {
            while (weight < 20) {
              weight = new Random().nextInt(60);
            }
          }
          
          edges.add(new Edge(currentPiece, board.get(col).get(row + 1), weight));
        }
        // if not leftmost column adds a leftwards edge with a random weight to edges
        if (col > 0) {
          // if bias is horizontal
          if (bias == 1) {
            while (weight < 20) {
              weight = new Random().nextInt(60);
            }
          }
          
          edges.add(new Edge(currentPiece, board.get(col - 1).get(row), weight));
        }
        // if not rightmost column adds a rightwards edge with a random weight to edges
        if (col < width - 1) {
          // if bias is horizontal
          if (bias == 1) {
            while (weight < 20) {
              weight = new Random().nextInt(60);
            }
          }
          
          edges.add(new Edge(currentPiece, board.get(col + 1).get(row), weight));
        }
        
        // adds the coords of this node to coords
        coords.add(new CartPt(row, col));
      }
    }
    
    // run kruskal's algorithm to make a new mst
    return new Kruskal(coords, edges).run();
  }
  
  // increments ticks
  public void onTick() {
    if (!this.endGame()) {
      this.ticks++;
    }
  }
  
}

// represents a game piece
class GamePiece {
  
  // coordinates
  int row;
  int col;
  
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;
  
  // initializes a random GamePiece that does not have the power station
  GamePiece(int x, int y) {
    this(new Random().nextBoolean(), new Random().nextBoolean(), 
        new Random().nextBoolean(), new Random().nextBoolean(), x, y);
    // makes sure the GamePiece has at least one connection piece
    while (!left && !right && !top && !bottom) {
      this.left = new Random().nextBoolean();
      this.right = new Random().nextBoolean();
      this.top = new Random().nextBoolean();
      this.bottom = new Random().nextBoolean();
    }
  }
  
  // initializes a GamePiece with given values and powered and powerStation set to false
  GamePiece(boolean left, boolean right, boolean top, boolean bottom, int col, int row) {
    this(left, right, top, bottom, col, row, false);
  }
  
  // initializes a GamePiece that can also change powered and powerStation
  GamePiece(boolean left, boolean right, boolean top, boolean bottom, int col, int row, 
      boolean powerStation) {
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;
    this.col = col;
    this.row = row;
  }
  
  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the power station
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);
   
    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.powerStation) {
      image = new OverlayImage(
                  new OverlayImage(
                      new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
                      new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
                  image);
    }
    return image;
  }
  
  // rotates the GamePiece 90 degrees counterclockwise
  public void rotate() {
    // straight horizontal line -> straight vertical line
    if (left && right && !top && !bottom) {
      this.left = false;
      this.right = false;
      this.top = true;
      this.bottom = true;
    }
    // straight vertical line -> straight horizontal line
    else if (!left && !right && top && bottom) {
      this.left = true;
      this.right = true;
      this.top = false;
      this.bottom = false;
    }
    // upwards pointing T shape -> leftwards pointing t shape
    else if (left && right && top && !bottom) {
      this.bottom = true;
      this.right = false;
    }
    // downwards pointing T shape -> rightwards pointing t shape
    else if (left && right && !top && bottom) {
      this.left = false;
      this.top = true;
    }
    // leftwards pointing T shape -> downwards pointing t shape
    else if (left && !right && top && bottom) {
      this.top = false;
      this.right = true;
    }
    // rightwards pointing T shape -> upwards pointing t shape
    else if (!left && right && top && bottom) {
      this.left = true;
      this.bottom = false;
    }
    // bottom right corner shape -> top right corner shape
    else if (left && !right && top && !bottom) {
      this.top = false;
      this.bottom = true;
    }
    // top right corner shape -> top left corner shape
    else if (left && !right && !top && bottom) {
      this.right = true;
      this.left = false;
    }
    // bottom left corner shape -> bottom right corner shape
    else if (!left && right && top && !bottom) {
      this.left = true;
      this.right = false;
    }
    // top left corner shape -> bottom left corner shape
    else if (!left && right && !top && bottom) {
      this.top = true;
      this.bottom = false;
    }
    // only left -> only bottom
    else if (left && !right && !top && !bottom) {
      this.left = false;
      this.bottom = true;
    }
    // only right -> only top
    else if (!left && right && !top && !bottom) {
      this.right = false;
      this.top = true;
    }
    // only top -> only left
    else if (!left && !right && top && !bottom) {
      this.top = false;
      this.left = true;
    }
    // only bottom -> only right
    else if (!left && !right && !top && bottom) {
      this.bottom = false;
      this.right = true;
    }
  }
  
  // checks if the piece is powered and updates it
  // this is basically breadth first search
  public void power(ArrayList<ArrayList<GamePiece>> board) {
    if (this.powerStation) {
      this.powered = true;
    }
    if (this.powered) {
      // go up
      // checks for: the piece can go up, this piece points up, 
      // upper piece points down, upper piece is not powered
      if (row > 0 && this.top && board.get(row - 1).get(col).bottom 
          && !board.get(row - 1).get(col).powered) {
        board.get(row - 1).get(col).powered = true;
        board.get(row - 1).get(col).power(board);
      }
      // go down
      // checks for: the piece can go down, this piece points down, 
      // lower piece points up, lower piece is not powered
      if (row < board.size() - 1 && this.bottom && board.get(row + 1).get(col).top 
          && !board.get(row + 1).get(col).powered) {
        board.get(row + 1).get(col).powered = true;
        board.get(row + 1).get(col).power(board);
      }
      // go left
      // checks for: the piece can go left, this piece points left, 
      // left piece points right, left piece is not powered
      if (col > 0 && this.left && board.get(row).get(col - 1).right
          && !board.get(row).get(col - 1).powered) {
        board.get(row).get(col - 1).powered = true;
        board.get(row).get(col - 1).power(board);
      }
      // go right
      // checks for: the piece can go right, this piece points right, 
      // right piece points left, right piece is not powered
      if (col < board.get(0).size() - 1 && this.right && board.get(row).get(col + 1).left
          && !board.get(row).get(col + 1).powered) {
        board.get(row).get(col + 1).powered = true;
        board.get(row).get(col + 1).power(board);
      }
    }
  }
  
  // creates a CartPt to represent coordinates
  public CartPt pointCoords() {
    return new CartPt(col, row);
  }
  
}

// represents an edge 
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;
  
  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
  
}

// represents a cartesian point
class CartPt {
  int x;
  int y;
  
  CartPt(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  // checks if two CartPts are equal
  public boolean equals(Object o) {
    if (!(o instanceof CartPt)) {
      return false;
    }
    CartPt other = (CartPt) o;
    return this.x == other.x && this.y == other.y;
  }
  
  // makes a new hashCode so that CartPts with the same x and y 
  // will be equal
  public int hashCode() {
    int result = 200;
    result = 200 * result + x;
    result = 200 * result + y;
    return result;
  }
  
}

// represents/executes kruskal's algorithm
class Kruskal {
  // coordinates of edges
  ArrayList<CartPt> nodeCoords;
  // arraylist of edges
  ArrayList<Edge> edgeList;
  // representatives for the edges
  HashMap<CartPt, CartPt> representatives;
  
  Kruskal(ArrayList<CartPt> nodeCoords, ArrayList<Edge> edgeList) {
    this.nodeCoords = nodeCoords;
    
    //sorts the list of edges by weight
    edgeList.sort(new WeightComparator());
    this.edgeList = edgeList;
    
    // initializes representatives to reference itself
    this.representatives = new HashMap<CartPt, CartPt>();
    for (CartPt p : nodeCoords) {
      representatives.put(p, p);
    }
    
  }
  
  // runs kruskal's algorithm
  public ArrayList<Edge> run() {
    
    // the tree that will contain all the edges and be returned in the end
    ArrayList<Edge> tree = new ArrayList<Edge>();
    
    // makes sure there are multiple edges so a tree can actually be made
    // runs through the edgeList until there are no edges left to add
    while (representatives.size() > 1 && !(edgeList.isEmpty())) {
      Edge next = edgeList.remove(0);
      
      // if the pointCoords of fromNode and the pointCoords of toNode aren't the same
      // (that is, fromNode and toNode aren't the exact same node) 
      // add them to the tree
      if (!(this.find(next.fromNode.pointCoords()).equals(this.find(next.toNode.pointCoords())))) {
        tree.add(next);
        // joins the two nodes together
        this.join(next.fromNode, next.toNode);
      }
    }
    // returns the completed tree
    return tree;
  }
  
  // joins two GamePieces together if they have same representative
  public void join(GamePiece left, GamePiece right) {
    representatives.put(this.find(left.pointCoords()), this.find(right.pointCoords()));
  }
  
  // finds the representative given a CartPt and updates it
  public CartPt find(CartPt pieceCord) {
    CartPt proxy = representatives.get(pieceCord);
    // if the CartPt given doesn't correspond to proxy
    // adds the CartPt to the representatives hash where proxy is
    if (!(proxy.equals(pieceCord))) {
      representatives.put(pieceCord, find(proxy));
    }
    return representatives.get(pieceCord);
  }
  
}

// compares two edges by weight
class WeightComparator implements Comparator<Edge> {
  
  //compares two edges weights
  public int compare(Edge edge1, Edge edge2) {
    return edge1.weight - edge2.weight;
  }
  
}

// LightEmAll test class
class LightEmAllExamples {
  GamePiece gpall;
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  GamePiece gp7;
  
  LightEmAll lea1;
  LightEmAll lea2;
  LightEmAll lea3;
  
  Kruskal test;
  
  // initialize
  void init() {
    // +
    gpall = new GamePiece(true, true, true, true, 0, 0);
    // left pointing T
    gp1 = new GamePiece(true, false, true, true, 0, 0);
    // horizontal line
    gp2 = new GamePiece(true, true, false, false, 0, 0);
    // bottom left corner
    gp3 = new GamePiece(false, true, true, false, 0, 0);
    
    // bottom only
    gp4 = new GamePiece(false, false, false, true, 0, 0);
    // top only
    gp5 = new GamePiece(false, false, true, false, 0, 0);
    // right pointing T
    gp6 = new GamePiece(false, true, true, true, 0, 0);
    // vertical line
    gp7 = new GamePiece(false, false, true, true, 0, 0);
    
    // 5x5 
    ArrayList<ArrayList<GamePiece>> arg = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
            new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, false, true, 0, 0),
                new GamePiece(false, false, false, true, 1, 0), 
                new GamePiece(false, false, false, true, 2, 0),
                new GamePiece(false, false, false, true, 3, 0), 
                new GamePiece(false, false, false, true, 4, 0))), 
            new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, true, 0, 1), 
                new GamePiece(false, false, true, true, 1, 1), 
                new GamePiece(false, false, true, true, 2, 1), 
                new GamePiece(false, false, true, true, 3, 1), 
                new GamePiece(false, false, true, true, 4, 1))), 
            new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, true, true, true, 0, 2), 
                new GamePiece(true, true, true, true, 1, 2), 
                new GamePiece(true, true, true, true, 2, 2), 
                new GamePiece(true, true, true, true, 3, 2), 
                new GamePiece(true, false, true, true, 4, 2))), 
            new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, true, 0, 3), 
                new GamePiece(false, false, true, true, 1, 3), 
                new GamePiece(false, false, true, true, 2, 3), 
                new GamePiece(false, false, true, true, 3, 3), 
                new GamePiece(false, false, true, true, 4, 3))), 
            new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, false, 0, 4), 
                new GamePiece(false, false, true, false, 1, 4), 
                new GamePiece(false, false, true, false, 2, 4), 
                new GamePiece(false, false, true, false, 3, 4), 
                new GamePiece(false, false, true, false, 4, 4)))));
    lea1 = new LightEmAll(arg, 2, 2);
    lea2 = new LightEmAll(3,3,0);
    lea3 = new LightEmAll(new ArrayList<ArrayList<GamePiece>>(Arrays.asList(
        new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, false, true, 0, 0),
            new GamePiece(false, false, false, true, 1, 0), 
            new GamePiece(false, false, false, true, 2, 0),
            new GamePiece(false, false, false, true, 3, 0), 
            new GamePiece(false, false, false, true, 4, 0))), 
        new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, true, 0, 1), 
            new GamePiece(false, false, true, true, 1, 1), 
            new GamePiece(false, false, true, true, 2, 1), 
            new GamePiece(false, false, true, true, 3, 1), 
            new GamePiece(false, false, true, true, 4, 1))), 
        new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, true, true, true, 0, 2), 
            new GamePiece(true, true, true, true, 1, 2), 
            new GamePiece(true, true, true, true, 2, 2), 
            new GamePiece(true, true, true, true, 3, 2), 
            new GamePiece(true, false, true, true, 4, 2))), 
        new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, true, 0, 3), 
            new GamePiece(false, false, true, true, 1, 3), 
            new GamePiece(false, false, true, true, 2, 3), 
            new GamePiece(true, true, false, false, 3, 3), 
            new GamePiece(false, false, true, true, 4, 3))), 
        new ArrayList<GamePiece>(Arrays.asList(new GamePiece(false, false, true, false, 0, 4), 
            new GamePiece(false, false, true, false, 1, 4), 
            new GamePiece(false, false, true, false, 2, 4), 
            new GamePiece(false, false, true, false, 3, 4), 
            new GamePiece(false, false, true, false, 4, 4))))), 2, 2);
    
    test = new Kruskal(new ArrayList<CartPt>(Arrays.asList(new CartPt(0,0), new CartPt(1,1), 
        new CartPt(1, 2))), new ArrayList<Edge>(Arrays.asList(new Edge(gpall, gp1, 10), 
            new Edge(gp3, gp2, 30), new Edge(gp1, gp2, 560))));
  }
  
  // tests rotate method
  void testRotate(Tester t) {
    this.init();
    this.gpall.rotate();
    t.checkExpect(gpall, new GamePiece(true, true, true, true, 0, 0));
    this.gp1.rotate();
    t.checkExpect(gp1, new GamePiece(true, true, false, true, 0, 0));
    this.gp2.rotate();
    t.checkExpect(gp2, new GamePiece(false, false, true, true, 0, 0));
    this.gp3.rotate();
    t.checkExpect(gp3, new GamePiece(true, false, true, false, 0, 0));
  }
  
  // tests makeScene method
  void testMakeScene(Tester t) {
    this.init();
    lea1.makeScene();
    WorldScene w = new WorldScene(490, 560);
    w.placeImageXY(new OverlayImage(new TextImage(Integer.toString(0), 70 / 2,
        Color.black), new OverlayImage(new RectangleImage(70, 70, OutlineMode.OUTLINE, 
        Color.black), new RectangleImage(70, 70, OutlineMode.SOLID, Color.gray))), 
        70 * (5 + 2) / 4, 70);
    w.placeImageXY(new OverlayImage(new TextImage("New Game", 70 / 6,
        Color.black), new OverlayImage(new RectangleImage(70, 70, OutlineMode.OUTLINE, 
        Color.black), new RectangleImage(70, 70, OutlineMode.SOLID, Color.gray))), 
        70 * (5 + 2) / 2, 70);
    w.placeImageXY(new OverlayImage(new TextImage("0", 
        70 / 2, Color.black), new OverlayImage(new RectangleImage(70, 70, 
            OutlineMode.OUTLINE, Color.black), new RectangleImage(70, 70, 
                OutlineMode.SOLID, Color.gray))), 3 * 70 * (5 + 2) / 4, 70);
    // row 1
    w.placeImageXY(this.lea1.board.get(0).get(0).tileImage(70, 14, Color.YELLOW, false), 105, 175);
    w.placeImageXY(this.lea1.board.get(0).get(1).tileImage(70, 14, Color.YELLOW, false), 175, 175);
    w.placeImageXY(this.lea1.board.get(0).get(2).tileImage(70, 14, Color.YELLOW, false), 245, 175);
    w.placeImageXY(this.lea1.board.get(0).get(3).tileImage(70, 14, Color.YELLOW, false), 315, 175);
    w.placeImageXY(this.lea1.board.get(0).get(4).tileImage(70, 14, Color.YELLOW, false), 385, 175);
    // row 2
    w.placeImageXY(this.lea1.board.get(1).get(0).tileImage(70, 14, Color.YELLOW, false), 105, 245);
    w.placeImageXY(this.lea1.board.get(1).get(1).tileImage(70, 14, Color.YELLOW, false), 175, 245);
    w.placeImageXY(this.lea1.board.get(1).get(2).tileImage(70, 14, Color.YELLOW, false), 245, 245);
    w.placeImageXY(this.lea1.board.get(1).get(3).tileImage(70, 14, Color.YELLOW, false), 315, 245);
    w.placeImageXY(this.lea1.board.get(1).get(4).tileImage(70, 14, Color.YELLOW, false), 385, 245);
    // row 3
    w.placeImageXY(this.lea1.board.get(2).get(0).tileImage(70, 14, Color.YELLOW, false), 105, 315);
    w.placeImageXY(this.lea1.board.get(2).get(1).tileImage(70, 14, Color.YELLOW, false), 175, 315);
    w.placeImageXY(this.lea1.board.get(2).get(2).tileImage(70, 14, Color.YELLOW, true), 245, 315);
    w.placeImageXY(this.lea1.board.get(2).get(3).tileImage(70, 14, Color.YELLOW, false), 315, 315);
    w.placeImageXY(this.lea1.board.get(2).get(4).tileImage(70, 14, Color.YELLOW, false), 385, 315);
    // row 4
    w.placeImageXY(this.lea1.board.get(3).get(0).tileImage(70, 14, Color.YELLOW, false), 105, 385);
    w.placeImageXY(this.lea1.board.get(3).get(1).tileImage(70, 14, Color.YELLOW, false), 175, 385);
    w.placeImageXY(this.lea1.board.get(3).get(2).tileImage(70, 14, Color.YELLOW, false), 245, 385);
    w.placeImageXY(this.lea1.board.get(3).get(3).tileImage(70, 14, Color.YELLOW, false), 315, 385);
    w.placeImageXY(this.lea1.board.get(3).get(4).tileImage(70, 14, Color.YELLOW, false), 385, 385);
    // row 5
    w.placeImageXY(this.lea1.board.get(4).get(0).tileImage(70, 14, Color.YELLOW, false), 105, 455);
    w.placeImageXY(this.lea1.board.get(4).get(1).tileImage(70, 14, Color.YELLOW, false), 175, 455);
    w.placeImageXY(this.lea1.board.get(4).get(2).tileImage(70, 14, Color.YELLOW, false), 245, 455);
    w.placeImageXY(this.lea1.board.get(4).get(3).tileImage(70, 14, Color.YELLOW, false), 315, 455);
    w.placeImageXY(this.lea1.board.get(4).get(4).tileImage(70, 14, Color.YELLOW, false), 385, 455);
    // places text
    w.placeImageXY(new TextImage("You win!", 70, Color.green), 245, 163);
      
    w.placeImageXY(new TextImage("Score: 0", 2 * 70 / 3, Color.green), 70 * (5 + 2) / 2, 
        70 * (5 + 2) / 2);
    w.placeImageXY(new TextImage("Best Score: " + Integer.toString(0), 70 / 2, Color.green), 
        70 * (5 + 2) / 2, 2 * 70 * (5 + 2) / 3);
    
    t.checkExpect(this.lea1.makeScene(), w);
    
  }
  
  // tests boardInit method
  void testBoardInit(Tester t) {
    this.init();
    // checks size is correct
    t.checkExpect(lea2.board.size(), 3);
    t.checkExpect(lea2.board.get(0).size(), 3);
  }
  
  // tests onMouseClicked method
  void testOnMouseClicked(Tester t) {
    this.init();
    lea1.onMouseClicked(new Posn(150, 220), "LeftButton");
    t.checkExpect(lea1.board.get(1).get(1), new GamePiece(true, true, false, false, 1, 1));
    lea1.onMouseClicked(new Posn(150, 220), "RightButton");
    t.checkExpect(lea1.board.get(1).get(1), new GamePiece(true, true, false, false, 1, 1));
  }
  
  // tests onKeyEvent method
  void testOnKeyEvent(Tester t) {
    this.init();
    lea3.onKeyEvent("right");
    t.checkExpect(lea3.board.get(2).get(2).powerStation, false);
    t.checkExpect(lea3.board.get(2).get(3).powerStation, true);
    lea3.onKeyEvent("left");
    t.checkExpect(lea3.board.get(2).get(2).powerStation, true);
    t.checkExpect(lea3.board.get(2).get(3).powerStation, false);
    lea3.onKeyEvent("up");
    t.checkExpect(lea3.board.get(2).get(2).powerStation, false);
    t.checkExpect(lea3.board.get(1).get(2).powerStation, true);
    lea3.onKeyEvent("down");
    t.checkExpect(lea3.board.get(2).get(2).powerStation, true);
    t.checkExpect(lea3.board.get(1).get(2).powerStation, false);
    // testing that it doesn't work for other keys
    lea3.onKeyEvent("A");
    t.checkExpect(lea3.board.get(2).get(2).powerStation, true);
  }
  
  // tests endGame method
  void testEndGame(Tester t) {
    this.init();
    lea1.makeScene();
    t.checkExpect(lea1.endGame(), true);
    t.checkExpect(lea3.endGame(), false);
  }
  
  // tests power method
  void testPower(Tester t) {
    this.init();
    // rotates a piece that should be powered so it isn't powered anymore
    lea1.board.get(0).get(0).rotate();
    lea1.board.get(0).get(0).power(lea1.board);
    t.checkExpect(lea1.board.get(0).get(0).powered, false);
  }
  
  // runs bigBang
  void testBigBang(Tester t) {
    this.init();
    // lea3.bigBang(1000, 1000, 0.01);
    lea2.bigBang(lea2.cellsize * (lea2.width + 2), lea2.cellsize * (lea2.height + 3), 1);
  }
  
  // tests createMST method from LightEmAll
  void testCreateMST(Tester t) {
    this.init();
    t.checkExpect(lea2.createMST(0).size(), 8);
  }
  
  // tests pointCoords from GamePiece
  void testPointCoords(Tester t) {
    this.init();
    t.checkExpect(this.lea2.board.get(0).get(0).pointCoords(), new CartPt(0,0));
  }
  
  // tests equals from CartPt
  void testEquals(Tester t) {
    t.checkExpect(new CartPt(0,0).equals(new CartPt(0,0)), true);
    t.checkExpect(new CartPt(0,0).equals(new CartPt(1,0)), false);
  }
  
  // tests hashCode from CartPt
  void testHashCode(Tester t) {
    t.checkExpect(new CartPt(0,0).hashCode(), 8000000);
  }
  
  // tests run from Kruskal
  void testRun(Tester t) {
    this.init();
    t.checkExpect(this.test.run(), new ArrayList<Edge>());
  }
  
  // tests join from Kruskal
  void testJoin(Tester t) {
    this.init();
    test.join(gp2, gp1);
    t.checkExpect(this.test.representatives.get(new CartPt(1,1)), new CartPt(1,1));
  }
  
  // tests find from Kruskal
  void testFind(Tester t) {
    this.init();
    test.join(gp1, gpall);
    t.checkExpect(test.find(new CartPt(1,1)), new CartPt(1,1));
  }
  
  // tests onTick from LightEmAll
  void testOnTick(Tester t) {
    this.init();
    t.checkExpect(this.lea2.ticks, 0);
    lea2.onTick();
    t.checkExpect(this.lea2.ticks, 1);
  }
