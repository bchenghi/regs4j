package core.git;

import model.HunkEntity;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


public class GitUtils {

    public static boolean clone(File localFile, String cloneUrl) {
        localFile.mkdir();
        try (Git result = Git.cloneRepository()
                .setURI(cloneUrl)
                .setProgressMonitor(new SimpleProgressMonitor())
                .setCloneAllBranches(true)
                .setDirectory(localFile)
                .call()) {
            return true;
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            return false;
        }
    }

    public static boolean checkout(String commitID, File codeDir) {
        try (Git git = Git.open(codeDir)) {
            git.checkout().setName(commitID).call();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public static String getBuggyIDByBfc1(String commitID, File codeDir) {
        String result ="";
        try (Repository repository = RepositoryProvider.getRepoFromLocal(codeDir); Git git = new Git(repository)) {
            if (commitID.contains("~1")){
                try (RevWalk revWalk = new RevWalk(repository);) {
                    RevCommit commit = revWalk.parseCommit(repository.resolve(commitID));
                    result = commit.getName();
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }

    }

    public static List<DiffEntry> getDiffEntriesBetweenCommits(File codeDir, String newID, String oldID) {
        try (Repository repository = RepositoryProvider.getRepoFromLocal(codeDir); Git git = new Git(repository)) {
             return git.diff().
                    setOldTree(prepareTreeParser(repository,oldID)).
                    setNewTree(prepareTreeParser(repository,newID)).
                    call();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static List<HunkEntity> getHunksBetweenCommits(File codeDir, String newID, String oldID) {
        List<HunkEntity> result = new LinkedList<>();
        try (Repository repository = RepositoryProvider.getRepoFromLocal(codeDir); Git git = new Git(repository);
             DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            List<DiffEntry> diffEntries = git.diff().
                    setOldTree(prepareTreeParser(repository,oldID)).
                    setNewTree(prepareTreeParser(repository,newID)).
                    call();
            diffFormatter.setRepository(repository);
            for (DiffEntry diffEntry: diffEntries) {
                FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);
                List<? extends HunkHeader> hunkHeaders = fileHeader.getHunks();
                for (HunkHeader hunk : hunkHeaders) {
                    for (Edit edit : hunk.toEditList()) {
                        HunkEntity hunkEntity = new HunkEntity();
                        hunkEntity.setOldPath(hunk.getFileHeader().getOldPath());
                        hunkEntity.setNewPath(hunk.getFileHeader().getNewPath());
                        hunkEntity.setBeginA(edit.getBeginA());
                        hunkEntity.setBeginB(edit.getBeginB());
                        hunkEntity.setEndA(edit.getEndA());
                        hunkEntity.setEndB(edit.getEndB());
                        hunkEntity.setType(edit.getType().toString());
                        result.add(hunkEntity);
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public static List<HunkEntity> getHunks(DiffEntry entry, File codeDir) {
        List<HunkEntity> result = new LinkedList<>();
        try (Repository repository = RepositoryProvider.getRepoFromLocal(codeDir); DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            FileHeader fileHeader = diffFormatter.toFileHeader(entry);
            List<? extends HunkHeader> hunkHeaders = fileHeader.getHunks();
            for (HunkHeader hunk : hunkHeaders) {
                for (Edit edit: hunk.toEditList()) {
                    HunkEntity hunkEntity = new HunkEntity();
                    hunkEntity.setOldPath(hunk.getFileHeader().getOldPath());
                    hunkEntity.setNewPath(hunk.getFileHeader().getNewPath());
                    hunkEntity.setBeginA(edit.getBeginA());
                    hunkEntity.setBeginB(edit.getBeginB());
                    hunkEntity.setEndA(edit.getEndA());
                    hunkEntity.setEndB(edit.getEndB());
                    hunkEntity.setType(edit.getType().toString());
                    result.add(hunkEntity);
                }

            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws Exception {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }


}
