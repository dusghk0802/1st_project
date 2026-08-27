package project;
/** Command-line Java vector indexer. Requires OPENAI_API_KEY in the environment. */
public final class JavaRagIndexer { public static void main(String[] args) throws Exception { System.out.println("Indexed="+new JavaRagService().syncAll()); } }
