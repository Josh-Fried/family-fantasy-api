package family.fantasy.api.core;

import family.fantasy.api.locks.Pick;
import family.fantasy.api.locks.PickRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Map;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final PickRepository pickRepository;
    private final NflStateService nflStateService;

    // Injects required repositories for managing groups, memberships, and user picks
    public GroupService(GroupRepository groupRepository, UserGroupRepository userGroupRepository, PickRepository pickRepository, NflStateService nflStateService) {
        this.groupRepository = groupRepository;
        this.userGroupRepository = userGroupRepository;
        this.pickRepository = pickRepository;
        this.nflStateService = nflStateService;
    }

    /**
     * Creates a new group and assigns the creator as an admin.
     * Spam Protection: Limits users to a maximum of 5 active groups per game type.
     */
    @CacheEvict(value = "groupDetails", allEntries = true)
    public Group createGroup(User user, String groupName, String gameType) {
        Group.GameType typeEnum = Group.GameType.fromString(gameType);
        
        long currentGroupCount = userGroupRepository.countActiveGroupsForUser(user.getId(), typeEnum);
        
        if (currentGroupCount >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Spam protection: You can only create up to 5 groups for " + gameType);
        }

        Group newGroup = new Group(groupName);
        newGroup.setGameType(typeEnum);
        Group savedGroup = groupRepository.save(newGroup);

        UserGroup userGroup = new UserGroup();
        userGroup.setUser(user);
        userGroup.setGroup(savedGroup);
        userGroup.setIsAdmin(true); 
        
        userGroupRepository.save(userGroup);

        return savedGroup;
    }

    /**
     * Allows a user to join an existing group using a unique invite code.
     * Evicts the 'groupDetails' cache so all members see updated group lists immediately.
     */
    @CacheEvict(cacheNames = {"groupDetails", "groupLeaderboards"}, allEntries = true)
    public Group joinGroup(User user, String inviteCode) {
        Group group = groupRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        // Add user as a regular member (isAdmin = false)
        UserGroup userGroup = new UserGroup(user, group, false);
        userGroupRepository.save(userGroup);

        return group;
    }

    /**
     * Retrieves all raw UserGroup membership records for a specific user.
     */
    public List<UserGroup> getUserGroups(Long userId) {
        return userGroupRepository.findByUserId(userId);
    }

    /**
     * Looks up a single group entity by its ID, returning null if not found.
     */
    public Group getGroupById(Long groupId) {
        return groupRepository.findById(groupId).orElse(null);
    }

    /**
     * Calculates and returns the full leaderboard for a group.
     * Computes total correct picks (score) and weekly streak for every member, then sorts descending by score.
     * Cached under 'groupLeaderboards' using the groupId as the key.
     */
    @Cacheable(value = "groupLeaderboards", key = "#groupId")
    public List<GroupController.LeaderboardDTO> getLeaderboard(Long groupId) {
        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);

        return members.stream().map(member -> {
            User user = member.getUser();
            List<Pick> userPicks = pickRepository.findByUserId(user.getId());

            long correctPicks = userPicks.stream()
                .filter(pick -> pick.getMatchup() != null && 
                                "STATUS_FINAL".equals(pick.getMatchup().getStatus()) && 
                                pick.getSelectedTeam() != null && 
                                pick.getSelectedTeam().equalsIgnoreCase(pick.getMatchup().getWinningTeam()))
                .count();

            int streak = calculateWeeklyStreak(userPicks);

            return new GroupController.LeaderboardDTO(
                user.getId(),
                user.getDisplayName(),
                (int) correctPicks,
                streak,
                member.getIsAdmin()
            );
        })
        .sorted((a, b) -> Integer.compare(b.score(), a.score()))
        .collect(Collectors.toList());
    }

    /**
     * Generates detailed group summary cards for a user's Home Page dashboard.
     * Evaluates member count, user rank, total points, and weekly streak for each group.
     * Cached under 'groupDetails' using a composite key of groupId and userId.
     */
    // Fixed the cache key to only use the parameter that actually exists!
    @Cacheable(value = "groupDetails", key = "#userId")
    public List<GroupController.GroupResponseDTO> getUserGroupsDetailed(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findByUserId(userId);

        return userGroups.stream().map(ug -> {
            Group group = ug.getGroup();
            
            // Get total members in this group
            int memberCount = userGroupRepository.findByGroupId(group.getId()).size();

            // Reuse the getLeaderboard method to find this user's current rank, points, and streak
            List<GroupController.LeaderboardDTO> leaderboard = getLeaderboard(group.getId());

            int userRank = 1;
            int totalPoints = 0;
            int streak = 0;

            // Locate the user's position in the group leaderboard
            for (int i = 0; i < leaderboard.size(); i++) {
                GroupController.LeaderboardDTO entry = leaderboard.get(i);
                if (entry.userId().equals(userId)) {
                    userRank = i + 1; // 1-based index ranking
                    totalPoints = entry.score();
                    streak = entry.streak();
                    break;
                }
            }

            return new GroupController.GroupResponseDTO(
                ug.getId(),
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                ug.getIsAdmin(),
                group.getGameType(),
                memberCount,
                userRank,
                totalPoints,
                streak
            );
        }).collect(Collectors.toList());
    }

    /**
     * Fetches a user's recent week-by-week placement history across all joined groups.
     * Computes weekly scores, compares them against group members, and formats ordinal ranks (1st, 2nd, etc.) with badges.
     */
    public List<UserController.RecentResultDTO> getRecentUserResults(Long userId, int limit) {
        List<UserController.RecentResultDTO> recentResults = new ArrayList<>();
        List<UserGroup> userGroups = userGroupRepository.findByUserId(userId);
        
        for (UserGroup ug : userGroups) {
            Group group = ug.getGroup();
            int currentWeek = nflStateService.getCurrentWeek(); // Replace with dynamic NflStateService week logic when connected
            
            // Evaluate the last 3 completed weeks
            for (int week = currentWeek - 1; week >= Math.max(1, currentWeek - 3); week--) {
                final int targetWeek = week;
                List<UserGroup> groupMembers = userGroupRepository.findByGroupId(group.getId());
                int totalMembers = groupMembers.size();
                
                List<Integer> allScores = new ArrayList<>();
                int myScore = 0;
                
                // Calculate correct picks for each member for this specific week
                for (UserGroup member : groupMembers) {
                    int memberScore = (int) pickRepository.findByUserId(member.getUser().getId()).stream()
                        .filter(p -> p.getMatchup() != null 
                                && p.getMatchup().getWeekNumber() == targetWeek
                                && "STATUS_FINAL".equals(p.getMatchup().getStatus())
                                && p.getSelectedTeam() != null 
                                && p.getSelectedTeam().equalsIgnoreCase(p.getMatchup().getWinningTeam()))
                        .count();
                    
                    allScores.add(memberScore);
                    if (member.getUser().getId().equals(userId)) {
                        myScore = memberScore;
                    }
                }
                
                // Ignore weeks that have no finalized scores
                if (allScores.stream().allMatch(score -> score == 0)) {
                    continue;
                }
                
                // Sort scores descending to determine rank
                allScores.sort((a, b) -> b.compareTo(a));
                
                int rank = allScores.indexOf(myScore) + 1; // Handles ties gracefully
                boolean isLastPlace = (rank == totalMembers && totalMembers > 3);
                
                recentResults.add(new UserController.RecentResultDTO(
                    group.getGameType().name(),
                    targetWeek,
                    group.getName(),
                    getPlacementIcon(rank, isLastPlace),
                    rank,
                    getPlaceSuffix(rank),
                    myScore
                ));
            }
        }
        
        // Return most recent weeks first, capped to the requested limit
        return recentResults.stream()
                .sorted((a, b) -> Integer.compare(b.week(), a.week()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Helper function: Returns the correct ordinal suffix for a numerical rank (e.g., 1st, 2nd, 3rd, 4th, 11th).
     */
    private String getPlaceSuffix(int rank) {
        if (rank % 100 >= 11 && rank % 100 <= 13) {
            return "th";
        }
        switch (rank % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }

    /**
     * Helper function: Returns the appropriate emoji badge based on placement rank and group size.
     */
    private String getPlacementIcon(int rank, boolean isLastPlace) {
        if (rank == 1) return "🥇";
        if (rank == 2) return "🥈";
        if (rank == 3) return "🥉";
        if (isLastPlace) return "👎"; // Special icon for last place in groups larger than 3
        return "🏅"; // Default icon for 4th and below
    }

    /**
     * Calculates a user's active weekly streak (e.g., +3 for winning 3 consecutive weeks, -2 for losing 2).
     * Groups finalized picks by week, sorts weeks descending, and checks if the user had at least 1 correct pick that week.
     */
    public int calculateWeeklyStreak(List<Pick> userPicks) {
        if (userPicks == null || userPicks.isEmpty()) return 0;

        // Group finished picks by week number
        Map<Integer, List<Pick>> picksByWeek = userPicks.stream()
            .filter(p -> p.getMatchup() != null && "STATUS_FINAL".equals(p.getMatchup().getStatus()))
            .collect(Collectors.groupingBy(p -> p.getMatchup().getWeekNumber()));

        if (picksByWeek.isEmpty()) return 0;

        // Sort weeks descending (most recent week first)
        List<Integer> sortedWeeks = picksByWeek.keySet().stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        int streak = 0;
        Boolean streakType = null; // true = win streak, false = loss streak

        for (Integer week : sortedWeeks) {
            List<Pick> weekPicks = picksByWeek.get(week);

            long correctCount = weekPicks.stream()
                .filter(p -> p.getSelectedTeam() != null && 
                             p.getSelectedTeam().equalsIgnoreCase(p.getMatchup().getWinningTeam()))
                .count();

            boolean isWeekWin = correctCount > 0;

            if (streakType == null) {
                streakType = isWeekWin;
                streak = 1;
            } else if (isWeekWin == streakType) {
                streak++;
            } else {
                break; // Streak is broken
            }
        }

        return (streakType != null && streakType) ? streak : -streak;
    }

    @CacheEvict(cacheNames = {"groupDetails", "groupLeaderboards"}, allEntries = true)
    public void removeMember(Long groupId, Long targetUserId, Long requesterUserId) {
        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        
        UserGroup requesterMembership = members.stream()
            .filter(ug -> ug.getUser().getId().equals(requesterUserId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group."));
            
        if (!requesterMembership.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group administrators can remove members.");
        }
        
        if (targetUserId.equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove yourself. Please use the leave group feature.");
        }
        
        UserGroup targetMembership = members.stream()
            .filter(ug -> ug.getUser().getId().equals(targetUserId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not in this group."));
            
        userGroupRepository.delete(targetMembership);
    }

    @CacheEvict(cacheNames = {"groupDetails", "groupLeaderboards"}, allEntries = true)
    public void deleteGroup(Long groupId, Long requesterUserId) {
        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        
        UserGroup requesterMembership = members.stream()
            .filter(ug -> ug.getUser().getId().equals(requesterUserId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group."));
            
        if (!requesterMembership.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group administrators can delete the group.");
        }
        
        // Delete all bridge table associations first to prevent foreign key constraint violations
        userGroupRepository.deleteAll(members);
        groupRepository.deleteById(groupId);
    }

    /**
     * Allows any user to leave a group. 
     * If the leaving user is an admin, another member is automatically promoted.
     * If no members remain, the group is deleted.
     */
    @CacheEvict(cacheNames = {"groupDetails", "groupLeaderboards"}, allEntries = true)
    public void leaveGroup(Long groupId, Long userId) {
        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        
        UserGroup leavingMembership = members.stream()
            .filter(ug -> ug.getUser().getId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this group."));
            
        boolean wasAdmin = leavingMembership.getIsAdmin();
        userGroupRepository.delete(leavingMembership);
        
        List<UserGroup> remainingMembers = members.stream()
            .filter(ug -> !ug.getUser().getId().equals(userId))
            .collect(Collectors.toList());
            
        if (remainingMembers.isEmpty()) {
            groupRepository.deleteById(groupId);
        } else if (wasAdmin) {
            boolean hasOtherAdmin = remainingMembers.stream().anyMatch(UserGroup::getIsAdmin);
            if (!hasOtherAdmin) {
                UserGroup newAdmin = remainingMembers.get(0);
                newAdmin.setIsAdmin(true);
                userGroupRepository.save(newAdmin);
            }
        }
    }

    /**
     * Allows an existing admin to grant admin privileges to another member.
     * Supports multiple administrators per group.
     */
    @CacheEvict(cacheNames = {"groupDetails", "groupLeaderboards"}, allEntries = true)
    public void promoteToAdmin(Long groupId, Long targetUserId, Long requesterUserId) {
        List<UserGroup> members = userGroupRepository.findByGroupId(groupId);
        
        UserGroup requester = members.stream()
            .filter(ug -> ug.getUser().getId().equals(requesterUserId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group."));
            
        if (!requester.getIsAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can promote other members.");
        }
        
        UserGroup target = members.stream()
            .filter(ug -> ug.getUser().getId().equals(targetUserId))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user is not in this group."));
            
        target.setIsAdmin(true);
        userGroupRepository.save(target);
    }
}